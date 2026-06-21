package com.apextv.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class NetworkMonitor(private val ctx: Context) {
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { onConnected?.invoke() }
        override fun onLost(network: Network) { onDisconnected?.invoke() }
    }

    fun start() {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(req, callback)
    }

    fun stop() {
        try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
    }
}
