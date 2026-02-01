package com.otzrlabs.payney.capture.data

import android.util.Log
import com.otzrlabs.payney.capture.data.network.ApiClient
import com.otzrlabs.payney.capture.data.network.IngestRequest
import java.io.IOException
import java.time.Instant
import java.util.UUID

/**
 * Single shared entry point for forwarding a captured SMS or notification to
 * /api/transactions/ingest. Both SmsReceiver and NotificationCaptureService
 * call through here rather than each building their own request.
 *
 * Delivery is durable: anything that can't be sent right now (no network, or a
 * server 5xx) is persisted to [CaptureOutbox] and retried later -- on the next
 * capture, and whenever the network comes back (see PayNeyCaptureApp). A capture
 * is therefore never silently lost just because the phone was offline when the
 * message arrived.
 *
 * Callers are responsible for checking [CapturePrefs.captureEnabled] and
 * [TokenStore.hasToken] *before* calling this, since the allowlist filtering
 * that decides whether something is even worth forwarding lives in the caller.
 */
object CaptureRepository {

    private const val TAG = "CaptureRepository"

    /**
     * [eventTimeMillis] is when the underlying event actually happened (SMS
     * delivery time, notification post time) — NOT "now". It rides along as the
     * ingest `timestamp` so the backend can date the transaction correctly even
     * when the capture spends hours in the offline outbox first.
     */
    suspend fun forwardCapture(sourceChannel: String, rawText: String, eventTimeMillis: Long? = null) {
        // Opportunistically drain anything queued from an earlier offline period
        // first, so ordering stays roughly chronological.
        flushOutbox()

        val timestamp = Instant.ofEpochMilli(eventTimeMillis ?: System.currentTimeMillis()).toString()
        if (trySend(sourceChannel, rawText, timestamp)) {
            CapturePrefs.lastSyncMillis = System.currentTimeMillis()
        } else {
            Log.w(TAG, "Ingest unreachable, queuing capture for later retry")
            CaptureOutbox.add(
                PendingCapture(
                    id = UUID.randomUUID().toString(),
                    sourceChannel = sourceChannel,
                    rawText = rawText,
                    timestamp = timestamp,
                )
            )
        }
    }

    /**
     * Attempts to deliver every queued capture, removing each one the server
     * accepts. Stops at the first item that still can't be delivered (the
     * network is presumably still down) and leaves the rest for the next try.
     * Safe to call from anywhere; it no-ops when the queue is empty.
     */
    suspend fun flushOutbox() {
        if (CaptureOutbox.isEmpty()) return

        var deliveredAny = false
        for (item in CaptureOutbox.all()) {
            val delivered = trySend(item.sourceChannel, item.rawText, item.timestamp)
            if (!delivered) break
            CaptureOutbox.remove(item.id)
            deliveredAny = true
        }
        if (deliveredAny) CapturePrefs.lastSyncMillis = System.currentTimeMillis()
    }

    /**
     * Sends one capture. Returns true when it's been dealt with and should NOT
     * be retried -- either the server accepted it (2xx, incl. dedupe) or it was
     * rejected for a reason retrying can't fix (4xx: bad token, unparseable).
     * Returns false only for transient failures worth retrying: no network, or a
     * server 5xx.
     */
    private suspend fun trySend(sourceChannel: String, rawText: String, timestamp: String): Boolean {
        return try {
            val response = ApiClient.apiService.ingestTransaction(
                IngestRequest(
                    sourceChannel = sourceChannel,
                    rawText = rawText,
                    timestamp = timestamp,
                )
            )
            when {
                response.isSuccessful -> true
                response.code() in 500..599 -> false // server-side, retry later
                else -> {
                    // 4xx -- not retryable; drop it rather than loop forever.
                    Log.w(TAG, "Ingest rejected (HTTP ${response.code()}), dropping")
                    true
                }
            }
        } catch (e: IOException) {
            false // offline / timeout -- retry later
        }
    }
}
