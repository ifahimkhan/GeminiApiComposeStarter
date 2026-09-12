package com.fahim.geminiApiComposeStarter.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}

/**
 * Lightweight connectivity observer.
 *
 * It does not perform network work in a callback. The callback only publishes
 * a Boolean connectivity state, keeping system callbacks fast.
 */
class NetworkMonitor(
    context: Context,
) : ConnectivityObserver {

    private val connectivityManager =
        context.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    override val isOnline: Flow<Boolean> =
        callbackFlow {
            fun publishCurrentState() {
                trySend(currentlyOnline())
            }

            val callback =
                object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(network: Network) {
                        publishCurrentState()
                    }

                    override fun onLost(network: Network) {
                        publishCurrentState()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        val connected =
                            networkCapabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_INTERNET
                            ) &&
                                networkCapabilities.hasCapability(
                                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                                )

                        trySend(connected)
                    }
                }

            publishCurrentState()

            connectivityManager.registerDefaultNetworkCallback(
                callback
            )

            awaitClose {
                runCatching {
                    connectivityManager.unregisterNetworkCallback(
                        callback
                    )
                }
            }
        }.distinctUntilChanged()

    private fun currentlyOnline(): Boolean {
        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) &&
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
    }
}
