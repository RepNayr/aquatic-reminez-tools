package com.repp.aquaticremineztools.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

/**
 * Helper untuk ambil info IP, Connection Type, dan Ping.
 */
object InfoCardHelper {

    /**
     * Ambil IP lokal (WiFi atau seluler).
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.") || ip.startsWith("10.") ||
                            ip.startsWith("172.") || ip.startsWith("100.")) {
                            return ip
                        }
                    }
                }
            }
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (_: Exception) { }
        return "Unavailable"
    }

    /**
     * Ambil info koneksi:
     * - WiFi → label "WI-FI" + SSID
     * - Cellular → label "DATA SELULER" + Operator
     */
    fun getConnectionInfo(context: Context): ConnectionInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return ConnectionInfo("OFFLINE", "", false)
        val caps = cm.getNetworkCapabilities(network)
            ?: return ConnectionInfo("OFFLINE", "", false)

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val ssid = getWifiSsid(context)
                ConnectionInfo("WI-FI", ssid, true)
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val operator = getCarrierName(context)
                ConnectionInfo("DATA SELULER", operator, true)
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                ConnectionInfo("ETHERNET", "", true)
            }
            else -> ConnectionInfo("UNKNOWN", "", false)
        }
    }

    private fun getWifiSsid(context: Context): String {
        return try {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            @Suppress("DEPRECATION")
            val ssid = info?.ssid?.replace("\"", "") ?: ""
            if (ssid.isEmpty() || ssid == "<unknown ssid>") "Connected" else ssid
        } catch (_: Exception) {
            "Connected"
        }
    }

    /**
     * Ambil nama operator (Telkomsel, XL Axiata, Indosat, dll).
     * Butuh permission READ_PHONE_STATE — dibungkus try-catch.
     */
    @SuppressLint("MissingPermission")
    private fun getCarrierName(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val name = tm.networkOperatorName
            if (name.isNullOrEmpty()) "Cellular" else name
        } catch (_: Exception) {
            "Cellular"
        }
    }

    /**
     * Ping ke host (ICMP dulu, fallback TCP port 53).
     */
    fun pingHost(host: String = "1.1.1.1", timeoutMs: Int = 3000): Long {
        try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(host).isReachable(timeoutMs)
            if (reachable) {
                return System.currentTimeMillis() - start
            }
        } catch (_: Exception) { }

        return try {
            val start = System.currentTimeMillis()
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, 53), timeoutMs)
            val elapsed = System.currentTimeMillis() - start
            socket.close()
            elapsed
        } catch (_: Exception) {
            -1
        }
    }

    data class ConnectionInfo(
        val type: String,
        val detail: String,
        val isConnected: Boolean,
    )
}