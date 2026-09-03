package com.example.smsgateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    fun isWifiConnected(context: Context): Boolean {
        return try {
            val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    fun localIpv4(context: Context): String {
        activeLanIpv4(context)?.let { return it }
        return fallbackLanIpv4() ?: "127.0.0.1"
    }

    fun baseUrl(context: Context, port: Int): String = "http://${localIpv4(context)}:$port"

    private fun activeLanIpv4(context: Context): String? {
        return try {
            val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
            val network = connectivity.activeNetwork ?: return null
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
            val isLan = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (!isLan) return null
            connectivity.getLinkProperties(network)
                ?.linkAddresses
                ?.asSequence()
                ?.map { it.address }
                ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun fallbackLanIpv4(): String? {
        return try {
            val candidates = NetworkInterface.getNetworkInterfaces()?.asSequence().orEmpty()
                .filter { it.isUp && !it.isLoopback && isLikelyLanInterface(it.name) }
                .flatMap { networkInterface ->
                    networkInterface.inetAddresses.asSequence()
                        .filter { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                        .mapNotNull { address -> address.hostAddress?.let { networkInterface.name to it } }
                }
                .sortedBy { interfacePriority(it.first) }
                .toList()
            candidates.firstOrNull()?.second
        } catch (_: Exception) {
            null
        }
    }

    private fun isLikelyLanInterface(name: String): Boolean {
        val lower = name.lowercase()
        return lower.startsWith("wlan") ||
            lower.startsWith("eth") ||
            lower.startsWith("ap") ||
            lower.startsWith("swlan") ||
            lower.startsWith("rndis") ||
            lower.startsWith("usb")
    }

    private fun interfacePriority(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.startsWith("wlan") -> 0
            lower.startsWith("eth") -> 1
            lower.startsWith("ap") || lower.startsWith("swlan") -> 2
            lower.startsWith("rndis") || lower.startsWith("usb") -> 3
            else -> 10
        }
    }
}
