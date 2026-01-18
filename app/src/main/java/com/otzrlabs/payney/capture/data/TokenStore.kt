package com.otzrlabs.payney.capture.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Securely persists the device pairing token (an EncryptedSharedPreferences-backed
 * wrapper, not plain SharedPreferences -- this is the credential that authorizes
 * every request to the PayNey backend). Must be initialized once via [init]
 * before use, from [com.otzrlabs.payney.capture.PayNeyCaptureApp.onCreate].
 */
object TokenStore {

    private const val PREFS_FILE_NAME = "payney_capture_secure_prefs"
    private const val KEY_DEVICE_TOKEN = "device_token"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_DEVICE_TOKEN).apply()
    }
}
