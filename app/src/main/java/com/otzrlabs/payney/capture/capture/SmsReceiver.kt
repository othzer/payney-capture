package com.otzrlabs.payney.capture.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.otzrlabs.payney.capture.BuildConfig
import com.otzrlabs.payney.capture.data.CapturePrefs
import com.otzrlabs.payney.capture.data.CaptureRepository
import com.otzrlabs.payney.capture.data.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Forwards inbound SMS from allowlisted bank sender IDs to the backend.
 * Never forwards arbitrary personal SMS -- everything not matched by
 * [AllowList.isAllowedSmsSender] is discarded before it ever reaches
 * [CaptureRepository].
 */
class SmsReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Logged before any early-return so `adb logcat -s SmsReceiver` confirms
        // whether the broadcast is even reaching us (if you see nothing at all on
        // an incoming SMS, the RECEIVE_SMS permission isn't granted, or the OEM is
        // blocking the receiver — not an allowlist problem).
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "SMS_RECEIVED delivered (captureEnabled=${CapturePrefs.captureEnabled}, hasToken=${TokenStore.hasToken()})")
        }
        if (!CapturePrefs.captureEnabled || !TokenStore.hasToken()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // A single broadcast carries all PDU parts of one multi-part SMS from
        // one sender, so grouping by sender and concatenating bodies
        // reassembles the full message text.
        val partsBySender = messages.groupBy { it.originatingAddress.orEmpty() }

        // onReceive must return quickly or the system may consider it
        // unresponsive, but forwarding is a suspend network call -- goAsync()
        // keeps the receiver (and process) alive until the coroutine finishes.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, parts) in partsBySender) {
                    val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                    if (body.isBlank()) continue
                    val allowed = AllowList.isAllowedSmsSender(sender)
                    // If a bank SMS is being dropped, `adb logcat -s SmsReceiver`
                    // shows the exact sender string so it can be allowlisted.
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "SMS from '$sender' allowed=$allowed")
                    }
                    if (!allowed) continue
                    CaptureRepository.forwardCapture(
                        sourceChannel = "sms",
                        rawText = body,
                        // The SMSC delivery timestamp — when the bank actually
                        // sent the message, not when we got around to forwarding.
                        eventTimeMillis = parts.first().timestampMillis,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
