package com.otzrlabs.payney.capture.data.network

import kotlinx.serialization.Serializable

@Serializable
data class PairDeviceRequest(
    val code: String,
)

@Serializable
data class DeviceTokenResponse(
    val token: String,
    val deviceId: String? = null,
)

@Serializable
data class IngestRequest(
    val sourceChannel: String,
    val rawText: String,
    // Backend reads this as `timestamp` (see app/api/transactions/ingest); it
    // was previously sent as `capturedAt` and silently ignored server-side.
    val timestamp: String,
)

// Shape returned by POST /api/transactions/receipt -- the endpoint extracts
// fields for the user to confirm; it does NOT persist a transaction. All fields
// are nullable because the model returns {} when the image isn't a receipt.
@Serializable
data class ReceiptExtractResponse(
    val amount: Double? = null,
    val merchant: String? = null,
    val date: String? = null,
    val suggestedCategory: String? = null,
)

// Sent to POST /api/transactions/receipt/confirm after the user reviews (and
// optionally edits) the extracted fields, to persist the receipt into the
// Review queue as a pending transaction.
@Serializable
data class ReceiptConfirmRequest(
    val amount: Double,
    val merchant: String? = null,
    val date: String? = null,
    val category: String? = null,
)
