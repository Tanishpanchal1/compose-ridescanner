package com.example.jetpack_test1

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

@Serializable
data class AutomationResponse(
    val service: String,
    val vehicle_type: String,
    val price_estimate: Double,
    val eta_seconds: Int
)

class RideAutomationService {

    data class AutomationRequest(
        val pickup: LocationCoords,
        val dropoff: LocationCoords,
        val services: List<String> = listOf("uber", "ola", "rapido")
    )

    suspend fun getRealTimeRides(request: AutomationRequest): List<RideOption> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RideOption>()

        request.services.forEach { service ->
            try {
                val serviceData = extractRideData(service, request.pickup, request.dropoff)
                results.addAll(serviceData)
            } catch (e: Exception) {
                Log.e("AUTOMATION", "Failed to extract from $service: ${e.message}")
                // Fallback to static data for this service
                results.addAll(getStaticDataForService(service, request.pickup, request.dropoff))
            }
        }

        return@withContext results.sortedBy { it.price }
    }

    private suspend fun extractRideData(service: String, pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> {
        return when (service) {
            "uber" -> extractUberData(pickup, dropoff)
            "ola" -> extractOlaData(pickup, dropoff)
            "rapido" -> extractRapidoData(pickup, dropoff)
            else -> emptyList()
        }
    }

    private suspend fun extractUberData(pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> {
        return try {
            // Call your automation endpoint
            val automationUrl = "http://10.0.2.2:8080/extract-uber" // Use 10.0.2.2 for emulator
            val requestBody = JSONObject().apply {
                put("pickup_lat", pickup.lat)
                put("pickup_lng", pickup.lng)
                put("dropoff_lat", dropoff.lat)
                put("dropoff_lng", dropoff.lng)
            }

            val response = makeAutomationRequest(automationUrl, requestBody)
            parseUberResponse(response)
        } catch (e: Exception) {
            Log.e("UBER_EXTRACTION", "Error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun extractOlaData(pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> {
        return try {
            val automationUrl = "http://10.0.2.2:8080/extract-ola"
            val requestBody = JSONObject().apply {
                put("pickup_lat", pickup.lat)
                put("pickup_lng", pickup.lng)
                put("dropoff_lat", dropoff.lat)
                put("dropoff_lng", dropoff.lng)
            }

            val response = makeAutomationRequest(automationUrl, requestBody)
            parseOlaResponse(response)
        } catch (e: Exception) {
            Log.e("OLA_EXTRACTION", "Error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun extractRapidoData(pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> {
        return try {
            val automationUrl = "http://10.0.2.2:8080/extract-rapido"
            val requestBody = JSONObject().apply {
                put("pickup_lat", pickup.lat)
                put("pickup_lng", pickup.lng)
                put("dropoff_lat", dropoff.lat)
                put("dropoff_lng", dropoff.lng)
            }

            val response = makeAutomationRequest(automationUrl, requestBody)
            parseRapidoResponse(response)
        } catch (e: Exception) {
            Log.e("RAPIDO_EXTRACTION", "Error: ${e.message}")
            emptyList()
        }
    }

    private fun parseUberResponse(jsonResponse: String): List<RideOption> {
        val results = mutableListOf<RideOption>()
        try {
            val jsonArray = JSONArray(jsonResponse)

            for (i in 0 until jsonArray.length()) {
                val ride = jsonArray.getJSONObject(i)
                results.add(
                    RideOption(
                        company = "Uber",
                        type = ride.getString("vehicle_type"),
                        price = ride.getDouble("price_estimate"),
                        etaSeconds = ride.getInt("eta_seconds"),
                        deepLink = generateUberDeepLink(ride)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PARSING", "Error parsing Uber response: ${e.message}")
        }
        return results
    }

    private fun parseOlaResponse(jsonResponse: String): List<RideOption> {
        val results = mutableListOf<RideOption>()
        try {
            val jsonArray = JSONArray(jsonResponse)

            for (i in 0 until jsonArray.length()) {
                val ride = jsonArray.getJSONObject(i)
                results.add(
                    RideOption(
                        company = "Ola",
                        type = ride.getString("vehicle_type"),
                        price = ride.getDouble("price_estimate"),
                        etaSeconds = ride.getInt("eta_seconds"),
                        deepLink = generateOlaDeepLink(ride)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PARSING", "Error parsing Ola response: ${e.message}")
        }
        return results
    }

    private fun parseRapidoResponse(jsonResponse: String): List<RideOption> {
        val results = mutableListOf<RideOption>()
        try {
            val jsonArray = JSONArray(jsonResponse)

            for (i in 0 until jsonArray.length()) {
                val ride = jsonArray.getJSONObject(i)
                results.add(
                    RideOption(
                        company = "Rapido",
                        type = ride.getString("vehicle_type"),
                        price = ride.getDouble("price_estimate"),
                        etaSeconds = ride.getInt("eta_seconds"),
                        deepLink = generateRapidoDeepLink(ride)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PARSING", "Error parsing Rapido response: ${e.message}")
        }
        return results
    }

    private fun makeAutomationRequest(url: String, requestBody: JSONObject): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 15000
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e("HTTP_ERROR", "Response code: ${connection.responseCode}")
                throw IOException("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun generateUberDeepLink(ride: JSONObject): String {
        return try {
            val pickup = ride.getJSONObject("pickup_location")
            val dropoff = ride.getJSONObject("dropoff_location")
            "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=${pickup.getDouble("lat")}&pickup[longitude]=${pickup.getDouble("lng")}&dropoff[latitude]=${dropoff.getDouble("lat")}&dropoff[longitude]=${dropoff.getDouble("lng")}"
        } catch (e: Exception) {
            Log.e("DEEP_LINK", "Error generating Uber deep link: ${e.message}")
            "https://m.uber.com"
        }
    }

    private fun generateOlaDeepLink(ride: JSONObject): String {
        return try {
            val pickup = ride.getJSONObject("pickup_location")
            val dropoff = ride.getJSONObject("dropoff_location")
            "https://olacabs.com/?pickup=${pickup.getDouble("lat")},${pickup.getDouble("lng")}&dropoff=${dropoff.getDouble("lat")},${dropoff.getDouble("lng")}"
        } catch (e: Exception) {
            Log.e("DEEP_LINK", "Error generating Ola deep link: ${e.message}")
            "https://olacabs.com"
        }
    }

    private fun generateRapidoDeepLink(ride: JSONObject): String {
        return try {
            "https://rapido.bike/"
        } catch (e: Exception) {
            Log.e("DEEP_LINK", "Error generating Rapido deep link: ${e.message}")
            "https://rapido.bike/"
        }
    }

    private fun getStaticDataForService(service: String, pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> {
        return when (service) {
            "uber" -> listOf(
                RideOption(
                    company = "Uber",
                    type = "UberX",
                    price = 180.0,
                    etaSeconds = 45,
                    deepLink = "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=${pickup.lat}&pickup[longitude]=${pickup.lng}&dropoff[latitude]=${dropoff.lat}&dropoff[longitude]=${dropoff.lng}"
                ),
                RideOption(
                    company = "Uber",
                    type = "UberXL",
                    price = 240.0,
                    etaSeconds = 50,
                    deepLink = "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=${pickup.lat}&pickup[longitude]=${pickup.lng}&dropoff[latitude]=${dropoff.lat}&dropoff[longitude]=${dropoff.lng}"
                )
            )
            "ola" -> listOf(
                RideOption(
                    company = "Ola",
                    type = "Mini",
                    price = 160.0,
                    etaSeconds = 40,
                    deepLink = "https://olacabs.com/?pickup=${pickup.lat},${pickup.lng}&dropoff=${dropoff.lat},${dropoff.lng}"
                ),
                RideOption(
                    company = "Ola",
                    type = "Auto",
                    price = 120.0,
                    etaSeconds = 35,
                    deepLink = "https://olacabs.com/?pickup=${pickup.lat},${pickup.lng}&dropoff=${dropoff.lat},${dropoff.lng}"
                )
            )
            "rapido" -> listOf(
                RideOption(
                    company = "Rapido",
                    type = "Bike",
                    price = 80.0,
                    etaSeconds = 25,
                    deepLink = "https://rapido.bike/"
                ),
                RideOption(
                    company = "Rapido",
                    type = "Auto",
                    price = 110.0,
                    etaSeconds = 30,
                    deepLink = "https://rapido.bike/"
                )
            )
            else -> emptyList()
        }
    }
}
