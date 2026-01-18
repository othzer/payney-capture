package com.otzrlabs.payney.capture.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Local-only device settings -- deliberately plain (unencrypted) SharedPreferences,
 * as opposed to [TokenStore]. Nothing stored here is a credential: the capture
 * toggle just lets the user pause forwarding without unpairing, and last-sync is
 * a UI timestamp. Must be initialized once via [init] before use, from
 * [com.otzrlabs.payney.capture.PayNeyCaptureApp.onCreate].
 */
object CapturePrefs {

    private const val PREFS_FILE_NAME = "payney_capture_local_prefs"
    private const val KEY_CAPTURE_ENABLED = "capture_enabled"
    private const val KEY_LAST_SYNC_MILLIS = "last_sync_millis"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    var captureEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAPTURE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CAPTURE_ENABLED, value).apply()

    var lastSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC_MILLIS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_MILLIS, value).apply()

    val hasSyncedBefore: Boolean
        get() = lastSyncMillis > 0L
}
