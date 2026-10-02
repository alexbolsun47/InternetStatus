package com.example.internetstatus

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class NetworkType {
    WIFI,
    MOBILE
}

/**
 * Удерживает запрошенную Android-сеть активной,
 * пока выполняются наши проверки.
 */
class NetworkLease(
    val network: Network,
    private val connectivityManager: ConnectivityManager,
    private val callback: ConnectivityManager.NetworkCallback
) {

    private var released = false

    fun release() {
        if (released) return
        released = true

        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
            // Callback уже мог быть снят системой.
        }
    }
}

object NetworkProvider {

    /**
     * Запрашивает у Android конкретный транспорт:
     *
     * WIFI -> только Wi-Fi
     * MOBILE -> только мобильная сеть
     *
     * Важно:
     * мы НЕ используем NET_CAPABILITY_VALIDATED,
     * потому что при режиме "белого списка"
     * Android может считать интернет невалидированным,
     * хотя отдельные разрешённые сайты доступны.
     */
    suspend fun requestNetwork(
        context: Context,
        networkType: NetworkType,
        timeoutMs: Long = 7000L
    ): NetworkLease? {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val transportType = when (networkType) {
            NetworkType.WIFI ->
                NetworkCapabilities.TRANSPORT_WIFI

            NetworkType.MOBILE ->
                NetworkCapabilities.TRANSPORT_CELLULAR
        }

        val request = NetworkRequest.Builder()
            .addTransportType(transportType)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        return withTimeoutOrNull(timeoutMs) {

            suspendCancellableCoroutine { continuation ->

                val callback =
                    object : ConnectivityManager.NetworkCallback() {

                        override fun onAvailable(network: Network) {

                            if (continuation.isActive) {

                                val lease = NetworkLease(
                                    network = network,
                                    connectivityManager = connectivityManager,
                                    callback = this
                                )

                                continuation.resume(lease)
                            }
                        }

                        override fun onUnavailable() {

                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }

                continuation.invokeOnCancellation {
                    try {
                        connectivityManager.unregisterNetworkCallback(
                            callback
                        )
                    } catch (_: Exception) {
                    }
                }

                try {

                    connectivityManager.requestNetwork(
                        request,
                        callback
                    )

                } catch (_: Exception) {

                    if (continuation.isActive) {
                        continuation.resume(null)
                    }

                }
            }
        }
    }
}