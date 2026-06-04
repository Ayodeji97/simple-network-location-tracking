package com.danzucker.networklocationtracker.core.data.networktracker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkChecker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkCheckerImpl(
    context: Context,
) : NetworkChecker {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isDeviceConnected(): Flow<Boolean> = callbackFlow {
        // Track the set of networks that currently have internet, and report connected as
        // "at least one such network exists". We do NOT recompute from activeNetwork inside
        // onLost: at the moment a network is lost, the system can still report it as the active
        // network with the INTERNET capability cached, so that query lies and we'd never see
        // the disconnect. The callback delivers events on a single Handler thread, so the plain
        // set needs no extra synchronization.
        val networksWithInternet = mutableSetOf<android.net.Network>()

        fun emitConnected() {
            trySend(networksWithInternet.isNotEmpty())
        }

        // Seed the baseline so a device that starts the app already offline (e.g. airplane mode)
        // gets a value without waiting on a callback that may never come.
        connectivityManager.activeNetwork?.let { active ->
            if (connectivityManager.hasInternet(active)) networksWithInternet.add(active)
        }
        emitConnected()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                networksWithInternet.add(network)
                emitConnected()
            }

            override fun onLost(network: android.net.Network) {
                networksWithInternet.remove(network)
                emitConnected()
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    networksWithInternet.add(network)
                } else {
                    networksWithInternet.remove(network)
                }
                emitConnected()
            }
        }

        // registerNetworkCallback (not requestNetwork) — we want to observe, not provision.
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun ConnectivityManager.hasInternet(network: android.net.Network): Boolean =
        getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
