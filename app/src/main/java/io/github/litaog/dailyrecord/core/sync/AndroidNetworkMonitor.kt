package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class AndroidNetworkMonitor(context: Context) : AutoCloseable {
    private val connectivityManager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)
    private val mutableAvailability = MutableStateFlow(connectivityManager.isInternetAvailable())
    val availability: StateFlow<Boolean> = mutableAvailability
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()

        override fun onLost(network: Network) = refresh()

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refresh()

        private fun refresh() {
            mutableAvailability.value = connectivityManager.isInternetAvailable()
        }
    }

    init {
        runCatching {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                callback,
            )
            registered = true
        }
    }

    override fun close() {
        if (registered) {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            registered = false
        }
    }
}

private fun ConnectivityManager.isInternetAvailable(): Boolean {
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
