package com.otzrlabs.payney.capture

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.otzrlabs.payney.capture.data.CaptureOutbox
import com.otzrlabs.payney.capture.data.CaptureRepository
import com.otzrlabs.payney.capture.data.CapturePrefs
import com.otzrlabs.payney.capture.data.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PayNeyCaptureApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        CapturePrefs.init(this)
        CaptureOutbox.init(this)
        registerOutboxFlushOnNetwork()
    }

    // Drains any queued captures whenever an internet-capable network becomes
    // available. registerNetworkCallback also fires onAvailable for the current
    // network right after registration, so this covers "app launched while
    // already online with a backlog" too.
    private fun registerOutboxFlushOnNetwork() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (!TokenStore.hasToken() || CaptureOutbox.isEmpty()) return
                    appScope.launch { CaptureRepository.flushOutbox() }
                }
            },
        )
    }
}
