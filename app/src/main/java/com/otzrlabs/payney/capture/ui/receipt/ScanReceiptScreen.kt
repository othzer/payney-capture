package com.otzrlabs.payney.capture.ui.receipt

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otzrlabs.payney.capture.ui.theme.PayNeyColors
import com.otzrlabs.payney.capture.util.CameraCaptureUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScanReceiptScreen(
    onDone: () -> Unit,
    viewModel: ScanReceiptViewModel = viewModel(),
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { capturedSuccessfully ->
        if (capturedSuccessfully) {
            pendingCameraUri?.let(viewModel::onPhotoCaptured)
        }
        pendingCameraUri = null
    }

    fun launchCamera() {
        val uri = CameraCaptureUtil.createCaptureUri(context)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchCamera() }

    fun takePhoto() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Scan receipt", style = MaterialTheme.typography.headlineMedium)

        val capturedUri = viewModel.capturedImageUri
        if (capturedUri != null) {
            ReceiptThumbnail(uri = capturedUri)
        }

        val state = viewModel.uploadState

        // Saved: the receipt is now in the Review queue.
        if (state == ReceiptUploadState.Saved) {
            Text(
                text = "Saved — review and approve it in PayNey.",
                style = MaterialTheme.typography.bodyLarge,
                color = PayNeyColors.Primary,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
            return@Column
        }

        // Extracted / Saving: editable form so the user can fix OCR mistakes
        // before it lands in Review.
        if (state == ReceiptUploadState.Extracted || state == ReceiptUploadState.Saving) {
            val saving = state == ReceiptUploadState.Saving

            Text(
                text = "Review details",
                style = MaterialTheme.typography.titleMedium,
                color = PayNeyColors.Primary,
            )
            OutlinedTextField(
                value = viewModel.amountInput,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount (₹)") },
                singleLine = true,
                enabled = !saving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.merchantInput,
                onValueChange = viewModel::onMerchantChange,
                label = { Text("Merchant") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            )
            viewModel.suggestedCategory?.let { category ->
                DetailRow("Suggested category", category)
            }
            Text(
                text = "Saved as an expense — you can recategorize and approve it in PayNey.",
                style = MaterialTheme.typography.bodyMedium,
                color = PayNeyColors.OnSurfaceMuted,
            )
            viewModel.formError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { viewModel.save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save to Review")
                }
            }
            OutlinedButton(
                onClick = { takePhoto() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Retake photo")
            }
            return@Column
        }

        // Idle / Uploading / Error
        OutlinedButton(
            onClick = { takePhoto() },
            enabled = state != ReceiptUploadState.Uploading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (capturedUri == null) "Take photo" else "Retake photo")
        }

        if (state is ReceiptUploadState.Error) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        if (capturedUri != null) {
            Button(
                onClick = { viewModel.upload(context) },
                enabled = state != ReceiptUploadState.Uploading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state == ReceiptUploadState.Uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Upload")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = PayNeyColors.OnSurfaceMuted)
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ReceiptThumbnail(uri: Uri) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            CameraCaptureUtil.decodeSampledBitmap(context, uri, reqWidth = 480, reqHeight = 480)
        }
    }

    val decodedBitmap = bitmap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(PayNeyColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, PayNeyColors.Outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = "Captured receipt",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "Loading preview…",
                style = MaterialTheme.typography.bodyMedium,
                color = PayNeyColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
