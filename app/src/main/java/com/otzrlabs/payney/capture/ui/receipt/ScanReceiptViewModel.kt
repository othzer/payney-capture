package com.otzrlabs.payney.capture.ui.receipt

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otzrlabs.payney.capture.data.CapturePrefs
import com.otzrlabs.payney.capture.data.network.ApiClient
import com.otzrlabs.payney.capture.data.network.ReceiptConfirmRequest
import com.otzrlabs.payney.capture.util.CameraCaptureUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant

// Flow: Idle --upload--> Uploading --> Extracted (editable form) --save--> Saving --> Saved.
// A hard failure (network/HTTP/unreadable image) goes to Error, which offers a retake.
sealed interface ReceiptUploadState {
    data object Idle : ReceiptUploadState
    data object Uploading : ReceiptUploadState
    data object Extracted : ReceiptUploadState
    data object Saving : ReceiptUploadState
    data object Saved : ReceiptUploadState
    data class Error(val message: String) : ReceiptUploadState
}

class ScanReceiptViewModel : ViewModel() {

    var capturedImageUri by mutableStateOf<Uri?>(null)
        private set

    var uploadState by mutableStateOf<ReceiptUploadState>(ReceiptUploadState.Idle)
        private set

    // Editable fields, pre-filled from extraction. Amount and merchant are what
    // OCR most often gets wrong, so the user can correct them before saving.
    var amountInput by mutableStateOf("")
        private set
    var merchantInput by mutableStateOf("")
        private set
    var suggestedCategory by mutableStateOf<String?>(null)
        private set
    // Inline validation/save error shown within the form (distinct from the
    // hard Error state that replaces the whole flow).
    var formError by mutableStateOf<String?>(null)
        private set

    // Kept as-is from extraction and passed straight back on confirm; not edited.
    private var extractedDateIso: String? = null

    // When OCR can't read a date off the receipt, fall back to when the photo
    // was taken — closer to the actual purchase than whenever "Save" finally
    // succeeds (e.g. after retries on flaky network).
    private var capturedAtIso: String? = null

    fun onPhotoCaptured(uri: Uri) {
        capturedImageUri = uri
        capturedAtIso = Instant.now().toString()
        uploadState = ReceiptUploadState.Idle
        formError = null
    }

    fun onAmountChange(value: String) {
        // Digits with at most one decimal point.
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) amountInput = filtered
        formError = null
    }

    fun onMerchantChange(value: String) {
        merchantInput = value
        formError = null
    }

    fun upload(context: Context) {
        val uri = capturedImageUri ?: return

        viewModelScope.launch {
            uploadState = ReceiptUploadState.Uploading
            formError = null
            try {
                // EXIF-corrected + re-encoded off the main thread -- decoding and
                // compressing a multi-megapixel photo is too heavy for Main.
                val bytes = withContext(Dispatchers.IO) {
                    CameraCaptureUtil.readOrientedJpegBytes(context, uri)
                } ?: throw IOException("Couldn't read the captured photo")
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
                // Backend reads the multipart field named "file" (see
                // app/api/transactions/receipt); sending "image" got a 400.
                val part = MultipartBody.Part.createFormData("file", "receipt.jpg", requestBody)

                val extracted = ApiClient.apiService.uploadReceipt(part)
                val hasData = extracted.amount != null || !extracted.merchant.isNullOrBlank()
                if (!hasData) {
                    uploadState = ReceiptUploadState.Error(
                        "Couldn't read a receipt from that photo. Try retaking it in better light.",
                    )
                    return@launch
                }
                amountInput = extracted.amount?.let(::formatAmount) ?: ""
                merchantInput = extracted.merchant.orEmpty()
                extractedDateIso = extracted.date
                suggestedCategory = extracted.suggestedCategory
                uploadState = ReceiptUploadState.Extracted
            } catch (e: HttpException) {
                uploadState = ReceiptUploadState.Error("Upload failed (HTTP ${e.code()}). Please try again.")
            } catch (e: IOException) {
                uploadState = ReceiptUploadState.Error("Couldn't reach PayNey. Check your connection and try again.")
            }
        }
    }

    fun save() {
        val amount = amountInput.trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            formError = "Enter a valid amount"
            return
        }

        viewModelScope.launch {
            uploadState = ReceiptUploadState.Saving
            formError = null
            try {
                val response = ApiClient.apiService.confirmReceipt(
                    ReceiptConfirmRequest(
                        amount = amount,
                        merchant = merchantInput.trim().ifBlank { null },
                        date = extractedDateIso ?: capturedAtIso,
                        category = suggestedCategory,
                    ),
                )
                if (response.isSuccessful) {
                    // A confirmed receipt is a real captured transaction, so this
                    // is where "last sync" legitimately advances.
                    CapturePrefs.lastSyncMillis = System.currentTimeMillis()
                    uploadState = ReceiptUploadState.Saved
                } else {
                    uploadState = ReceiptUploadState.Extracted
                    formError = "Couldn't save (HTTP ${response.code()}). Please try again."
                }
            } catch (e: IOException) {
                uploadState = ReceiptUploadState.Extracted
                formError = "Couldn't reach PayNey. Check your connection and try again."
            }
        }
    }

    // 500.0 -> "500", 500.5 -> "500.5" so the pre-filled field reads naturally.
    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
