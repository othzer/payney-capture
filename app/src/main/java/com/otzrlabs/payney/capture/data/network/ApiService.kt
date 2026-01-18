package com.otzrlabs.payney.capture.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("api/device/pair")
    suspend fun pairDevice(@Body request: PairDeviceRequest): DeviceTokenResponse

    // Called only from CaptureRepository (SmsReceiver / NotificationCaptureService),
    // never directly from a screen -- see CaptureRepository.forwardCapture.
    @POST("api/transactions/ingest")
    suspend fun ingestTransaction(@Body request: IngestRequest): Response<Unit>

    @Multipart
    @POST("api/transactions/receipt")
    suspend fun uploadReceipt(@Part image: MultipartBody.Part): ReceiptExtractResponse

    // Persists the reviewed receipt into the Review queue as a pending transaction.
    @POST("api/transactions/receipt/confirm")
    suspend fun confirmReceipt(@Body request: ReceiptConfirmRequest): Response<Unit>
}
