package com.example.jetpack_test1

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caching service wrapper for ride automation
 * Reduces redundant API calls and improves response times for repeated requests
 *
 * Features:
 * - 2-minute cache timeout for fresh pricing data
 * - Location-based cache keys
 * - Automatic cache invalidation
 * - Transparent fallback to live data when cache misses
 */
class CachedAutomationService(private val realTimeService: RealTimeAutomationService) {

    // ============================================================================
    // CONFIGURATION
    // ============================================================================

    /**
     * Cache storage: Map of location key to (ride data, timestamp) pairs
     */
    private val cache = mutableMapOf<String, Pair<List<RideOption>, Long>>()

    /**
     * Cache timeout in milliseconds (2 minutes)
     * Balances data freshness with performance
     */
    private val cacheTimeout = 2 * 60 * 1000L

    // ============================================================================
    // PUBLIC METHODS
    // ============================================================================

    /**
     * Gets real-time ride data with caching support
     * Checks cache first, falls back to live data if cache miss or expired
     *
     * @param pickup Pickup location coordinates
     * @param dropoff Dropoff location coordinates
     * @return List of ride options (cached or fresh)
     */
    suspend fun getRealTimeRides(pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> = withContext(Dispatchers.IO) {
        // Generate cache key from coordinates
        val cacheKey = "${pickup.lat},${pickup.lng}-${dropoff.lat},${dropoff.lng}"
        val now = System.currentTimeMillis()

        // Check cache first
        cache[cacheKey]?.let { (cachedRides, timestamp) ->
            if (now - timestamp < cacheTimeout) {
                Log.d("CACHE", "✅ Using cached data for $cacheKey (age: ${(now - timestamp)/1000}s)")
                return@withContext cachedRides
            } else {
                Log.d("CACHE", "⏰ Cache expired for $cacheKey, fetching fresh data")
            }
        }

        // Get fresh data from real-time service
        Log.d("CACHE", "🔄 Fetching fresh ride data for $cacheKey")
        val rides = realTimeService.getRealTimeRides(pickup, dropoff)

        // Store in cache with current timestamp
        cache[cacheKey] = rides to now
        Log.d("CACHE", "💾 Cached ${rides.size} rides for $cacheKey")

        return@withContext rides
    }

    /**
     * Clears all cached data
     * Useful for forcing fresh data or memory cleanup
     */
    fun clearCache() {
        val clearedEntries = cache.size
        cache.clear()
        Log.d("CACHE", "🗑️ Cleared $clearedEntries cache entries")
    }

    /**
     * Gets cache statistics for debugging
     *
     * @return Map with cache size and oldest entry age
     */
    fun getCacheStats(): Map<String, Any> {
        val now = System.currentTimeMillis()
        val oldestEntry = cache.values.minByOrNull { it.second }

        return mapOf(
            "size" to cache.size,
            "oldest_entry_age_seconds" to if (oldestEntry != null) (now - oldestEntry.second) / 1000 else 0
        )
    }
}
