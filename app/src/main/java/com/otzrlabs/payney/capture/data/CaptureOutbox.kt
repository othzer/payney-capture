package com.otzrlabs.payney.capture.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PendingCapture(
    val id: String,
    val sourceChannel: String,
    val rawText: String,
    val timestamp: String,
)

/**
 * Durable on-device queue of captures that couldn't be delivered yet -- the
 * phone was offline, or the backend was down -- so nothing is lost between a
 * bank SMS / UPI notification arriving and the network coming back.
 *
 * Persisted as JSON in plain SharedPreferences (the raw text is the same data
 * that's about to be sent to the server; it isn't a credential like the token).
 * Survives process death. The backend dedupes on reference/amount/date, so
 * re-sending an item that actually did get through is harmless.
 *
 * Must be initialized once via [init] from
 * [com.otzrlabs.payney.capture.PayNeyCaptureApp.onCreate].
 */
object CaptureOutbox {

    private const val PREFS_FILE_NAME = "payney_capture_outbox"
    private const val KEY_ITEMS = "items"
    // Cap so a long offline stretch (or a persistently failing item) can't grow
    // storage without bound; oldest is dropped first.
    private const val MAX_ITEMS = 200

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun all(): List<PendingCapture> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<PendingCapture>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(item: PendingCapture) {
        val items = all().toMutableList()
        items.add(item)
        while (items.size > MAX_ITEMS) items.removeAt(0)
        save(items)
    }

    @Synchronized
    fun remove(id: String) {
        save(all().filterNot { it.id == id })
    }

    @Synchronized
    fun isEmpty(): Boolean = all().isEmpty()

    @Synchronized
    fun size(): Int = all().size

    private fun save(items: List<PendingCapture>) {
        prefs.edit().putString(KEY_ITEMS, json.encodeToString(items)).apply()
    }
}
