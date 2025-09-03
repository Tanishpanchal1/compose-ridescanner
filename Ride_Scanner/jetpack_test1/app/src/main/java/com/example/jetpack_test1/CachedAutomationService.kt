package com.example.jetpack_test1

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachedAutomationService(private val baseService: RideAutomationService) {
    private val cache = mutableMapOf<String, Pair<List<RideOption>, Long>>()
    private val cacheTimeout = 2 * 60 * 1000L // 2 minutes

    suspend fun getRealTimeRides(request: RideAutomationService.AutomationRequest): List<RideOption> = withContext(Dispatchers.IO) {
        val cacheKey = "${request.pickup.lat},${request.pickup.lng}-${request.dropoff.lat},${request.dropoff.lng}"
        val now = System.currentTimeMillis()

        // Check cache first
        cache[cacheKey]?.let { (cachedRides, timestamp) ->
            if (now - timestamp < cacheTimeout) {
                Log.d("CACHE", "Using cached data for $cacheKey")
                return@withContext cachedRides
            }
        }

        // Get fresh data
        val rides = baseService.getRealTimeRides(request)
        cache[cacheKey] = rides to now

        return@withContext rides
    }
}
