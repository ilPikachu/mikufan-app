package com.mikufan.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * This class checks for network connectivity of the user device
 * From: https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
 */
class ConnectivityCheck(private val context: Context) {

    //TODO: fix deprecated API
    val isConnected: Boolean
        get() {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            val activeNetwork = connectivityManager?.activeNetworkInfo
            return activeNetwork?.isConnectedOrConnecting ?: false
        }
}
