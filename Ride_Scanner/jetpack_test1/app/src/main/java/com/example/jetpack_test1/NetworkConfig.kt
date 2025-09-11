package com.example.jetpack_test1

import android.os.Build

// NetworkConfig.kt
object NetworkConfig {
    private fun getServerBaseUrl(): String {
        return when {
            // Running on emulator
            Build.FINGERPRINT.contains("generic") -> "http://10.0.2.2:8080"

            // Running on real device - use your Linux machine's IP
            // You need to update this IP address
            else -> "http://192.168.1.100:8080"
        }
    }

    fun getUberUrl() = "${getServerBaseUrl()}/extract-uber"
    fun getOlaUrl() = "${getServerBaseUrl()}/extract-ola"
    fun getRapidoUrl() = "${getServerBaseUrl()}/extract-rapido"
    fun getHealthUrl() = "${getServerBaseUrl()}/health"
}
