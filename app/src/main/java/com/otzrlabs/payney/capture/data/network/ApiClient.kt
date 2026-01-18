package com.otzrlabs.payney.capture.data.network

import com.otzrlabs.payney.capture.BuildConfig
import com.otzrlabs.payney.capture.data.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single shared Retrofit/OkHttp instance for the whole app. Screens and
 * background components alike should go through [apiService] rather than
 * building their own client -- this is what attaches the bearer token
 * automatically and keeps the base URL in one place.
 */
object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val authInterceptor = Interceptor { chain ->
        val token = TokenStore.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        // The receipt endpoint runs Gemini OCR server-side and regularly takes
        // ~10s; OkHttp's 10s defaults were timing out mid-flight ("Couldn't
        // reach PayNey" even though the server logged a 200). Give slow AI
        // responses room to complete.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    // Retrofit requires the base URL to end in "/" or it throws at startup --
    // BuildConfig.API_BASE_URL (from gradle.properties/local.properties) isn't
    // guaranteed to include one, so normalize it here rather than relying on
    // every possible config value getting it right.
    private val normalizedBaseUrl: String =
        BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(normalizedBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
