package com.otzrlabs.payney.capture.ui.status

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.otzrlabs.payney.capture.data.CapturePrefs
import com.otzrlabs.payney.capture.data.TokenStore
import com.otzrlabs.payney.capture.ui.theme.PayNeyColors

@Composable
fun StatusScreen(
    onUnpaired: () -> Unit,
    onScanReceipt: () -> Unit,
) {
    val context = LocalContext.current

    var smsGranted by remember { mutableStateOf(hasSmsPermission(context)) }
    var notificationAccessGranted by remember { mutableStateOf(hasNotificationAccess(context)) }
    var captureEnabled by remember { mutableStateOf(CapturePrefs.captureEnabled) }
    var lastSyncMillis by remember { mutableStateOf(CapturePrefs.lastSyncMillis) }

    // Permission/notification-access state and last-sync time can all change
    // while this screen isn't visible (system dialog, Settings, a background
    // capture landing) -- refresh on every resume rather than only on first
    // composition.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smsGranted = hasSmsPermission(context)
                notificationAccessGranted = hasNotificationAccess(context)
                lastSyncMillis = CapturePrefs.lastSyncMillis
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        smsGranted = result.values.all { it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Status", style = MaterialTheme.typography.headlineMedium)

        StatusCard(title = "Connection") {
            StatusRow(
                label = "Device",
                value = "Connected",
                valueIsPositive = true,
            )
            TextButton(
                onClick = {
                    TokenStore.clearToken()
                    onUnpaired()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Unpair")
            }
        }

        StatusCard(title = "SMS capture") {
            StatusRow(
                label = "Permission",
                value = if (smsGranted) "Granted" else "Not granted",
                valueIsPositive = smsGranted,
            )
            if (!smsGranted) {
                Button(
                    onClick = {
                        smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                        )
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Grant")
                }
            }
        }

        StatusCard(title = "Notification access") {
            StatusRow(
                label = "Permission",
                value = if (notificationAccessGranted) "Granted" else "Not granted",
                valueIsPositive = notificationAccessGranted,
            )
            if (!notificationAccessGranted) {
                Text(
                    text = "Find \"PayNey Capture\" in the list and enable it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PayNeyColors.OnSurfaceMuted,
                )
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Grant")
                }
            }
        }

        StatusCard(title = "Capture") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Capture enabled", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Pause without unpairing this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PayNeyColors.OnSurfaceMuted,
                    )
                }
                Switch(
                    checked = captureEnabled,
                    onCheckedChange = {
                        captureEnabled = it
                        CapturePrefs.captureEnabled = it
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = PayNeyColors.Primary),
                )
            }
            StatusRow(
                label = "Last sync",
                value = formatLastSync(lastSyncMillis),
                valueIsPositive = null,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onScanReceipt,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scan receipt")
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PayNeyColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, PayNeyColors.Outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueIsPositive: Boolean?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = when (valueIsPositive) {
                true -> PayNeyColors.Primary
                false -> MaterialTheme.colorScheme.error
                null -> PayNeyColors.OnSurfaceMuted
            },
        )
    }
}

private fun formatLastSync(lastSyncMillis: Long): String {
    if (lastSyncMillis <= 0L) return "Not synced yet"
    return DateUtils.getRelativeTimeSpanString(
        lastSyncMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}
