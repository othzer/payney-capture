package com.otzrlabs.payney.capture.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.otzrlabs.payney.capture.data.CapturePrefs
import com.otzrlabs.payney.capture.data.CaptureRepository
import com.otzrlabs.payney.capture.data.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Forwards notifications from allowlisted UPI/payment apps to the backend.
 * Requires the user to manually enable this app under
 * Settings > Notification access -- see the Status screen's "Grant" flow,
 * since that permission has no runtime-dialog equivalent.
 *
 * TODO: some OEMs (Xiaomi/MIUI, Oppo/ColorOS, etc.) aggressively kill
 * background listener services unless the app is manually exempted from
 * battery optimization. Nothing to build for that here -- it's a
 * real-device tuning problem to solve during testing, not something this
 * code architecture can work around.
 */
class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!CapturePrefs.captureEnabled || !TokenStore.hasToken()) return
        if (!AllowList.isAllowedNotificationPackage(sbn.packageName)) return

        // Skip the group-summary notification: its text is empty or a rollup
        // like "3 new" -- the actual per-transaction detail is on the child
        // notifications, which arrive as separate onNotificationPosted calls.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        // GPay (and most payment apps) put the full transaction line in
        // EXTRA_BIG_TEXT via BigTextStyle; EXTRA_TEXT on its own is frequently a
        // truncated preview or empty, which is why these were being missed.
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val body = bigText.ifBlank { text }
        val combined = listOf(title, body).filter { it.isNotBlank() }.joinToString(separator = "\n")
        if (combined.isBlank()) return

        serviceScope.launch {
            CaptureRepository.forwardCapture(sourceChannel = "notification", rawText = combined)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
