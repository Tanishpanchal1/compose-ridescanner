package com.example.jetpack_test1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.android.gms.location.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NominatimPlace(
    val display_name: String,
    val lat: String,
    val lon: String
)

data class LocationCoords(val lat: Double, val lng: Double, val name: String)

data class RideOption(
    val company: String,
    val type: String,
    val price: Double,
    val etaSeconds: Int,
    val deepLink: String = ""
)

suspend fun fetchOpenStreetMapSuggestions(query: String): List<NominatimPlace> {
    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    Log.d("OSM_DEBUG", "Request URL: https://nominatim.openstreetmap.org/search?format=json&q=${Uri.encode(query)}&limit=5")

    return try {
        if (query.isBlank() || query.length < 3) return emptyList()
        Log.d("OSM_API", "Making request for: '$query'")

        val result: List<NominatimPlace> = client.get("http://nominatim.openstreetmap.org/search") {
            parameter("format", "json")
            parameter("q", query)
            parameter("limit", "5")
            parameter("addressdetails", "1")
            headers {
                append("User-Agent", "RideScanner/1.0 (android.app)")
            }
        }.body()

        Log.d("OSM_DEBUG", "Raw response: $result")
        Log.d("OSM_DEBUG", "Response size: ${result.size}")
        Log.d("OSM_API", "SUCCESS: Found ${result.size} suggestions")
        result.forEach { place ->
            Log.d("OSM_API", "Place: ${place.display_name}")
        }
        result
    } catch (e: Exception) {
        Log.e("OSM_API", "Error: ${e.message}", e)
        emptyList()
    } finally {
        client.close()
    }
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context, onResult: (lat: Double, lng: Double) -> Unit, onError: (String) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location: Location? = result.lastLocation
                    if (location != null) {
                        Log.d("LOCATION", "Got location: ${location.latitude}, ${location.longitude}")
                        onResult(location.latitude, location.longitude)
                    } else {
                        Log.e("LOCATION", "Location is null")
                        onError("Could not get location")
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        Log.e("LOCATION", "Location not available")
                        onError("Location not available")
                    }
                }
            }, Looper.getMainLooper()
        )
    } catch (e: Exception) {
        Log.e("LOCATION", "Location error: ${e.message}", e)
        onError("Location error: ${e.message}")
    }
}

// Fallback function for when automation fails
fun generateStaticRideData(pickup: LocationCoords, dropoff: LocationCoords): List<RideOption> {
    return listOf(
        RideOption(
            company = "Uber",
            type = "sedan",
            price = 180.0,
            etaSeconds = 45,
            deepLink = "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=${pickup.lat}&pickup[longitude]=${pickup.lng}&dropoff[latitude]=${dropoff.lat}&dropoff[longitude]=${dropoff.lng}"
        ),
        RideOption(
            company = "Ola",
            type = "auto",
            price = 220.0,
            etaSeconds = 50,
            deepLink = "https://olacabs.com/?pickup=${pickup.lat},${pickup.lng}&dropoff=${dropoff.lat},${dropoff.lng}"
        ),
        RideOption(
            company = "Namma Yatri",
            type = "sedan",
            price = 260.0,
            etaSeconds = 48,
            deepLink = "https://www.namayatri.com/"
        ),
        RideOption(
            company = "Rapido",
            type = "bike",
            price = 120.0,
            etaSeconds = 35,
            deepLink = "https://rapido.bike/"
        )
    )
}

fun launchRideApp(context: Context, deepLink: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("DEEP_LINK", "Error launching app: ${e.message}")
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize automation service with caching
    val automationService = remember {
        CachedAutomationService(RideAutomationService())
    }

    // Permission state
    var locationPermissionGranted by remember { mutableStateOf(false) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            locationPermissionGranted = granted
            Log.d("PERMISSIONS", "Location permission granted: $granted")
        }

    // Location state
    var currentCoords by remember { mutableStateOf<LocationCoords?>(null) }
    var displayCurrentLoc by remember { mutableStateOf("Getting location...") }
    var loadingLoc by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf("") }

    // Destination state
    var destination by remember { mutableStateOf("") }
    var destinationCoords by remember { mutableStateOf<LocationCoords?>(null) }
    var suggestions by remember { mutableStateOf<List<NominatimPlace>>(emptyList()) }
    var expandedSuggestions by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<RideOption>>(emptyList()) }

    // Automation status
    var automationStatus by remember { mutableStateOf("Ready") }
    var usingFallback by remember { mutableStateOf(false) }

    // Request permission on launch
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Get location when permission granted
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            loadingLoc = true
            locationError = ""
            displayCurrentLoc = "Getting location..."
            getCurrentLocation(
                context = context,
                onResult = { lat, lng ->
                    currentCoords = LocationCoords(lat, lng, "Current Location")
                    displayCurrentLoc = "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
                    loadingLoc = false
                    Log.d("LOCATION_SUCCESS", "Location set: $lat, $lng")
                },
                onError = { error ->
                    locationError = error
                    displayCurrentLoc = "Location error"
                    loadingLoc = false
                    Log.e("LOCATION_ERROR", "Error: $error")
                }
            )
        } else {
            displayCurrentLoc = "Location permission denied"
        }
    }

    // Autocomplete search
    LaunchedEffect(destination) {
        if (destination.isBlank()) {
            suggestions = emptyList()
            expandedSuggestions = false
            isSearching = false
            return@LaunchedEffect
        }

        if (destination.length < 3) {
            Log.d("AUTOCOMPLETE", "Query too short: '$destination'")
            return@LaunchedEffect
        }

        isSearching = true
        delay(800) // Debounce
        Log.d("AUTOCOMPLETE", "Starting search for: '$destination'")

        coroutineScope.launch {
            try {
                val newSuggestions = fetchOpenStreetMapSuggestions(destination)
                suggestions = newSuggestions
                expandedSuggestions = newSuggestions.isNotEmpty()
                Log.d("AUTOCOMPLETE", "Search complete: ${newSuggestions.size} results")
            } catch (e: Exception) {
                Log.e("AUTOCOMPLETE", "Search error: ${e.message}", e)
                suggestions = emptyList()
                expandedSuggestions = false
            } finally {
                isSearching = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp)
            ) {
                // Input form
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(16.dp))
                        .background(Color(0xFF272727), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Current location field
                        OutlinedTextField(
                            value = displayCurrentLoc,
                            onValueChange = {},
                            label = { Text("Current Location") },
                            enabled = false,
                            isError = locationError.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (loadingLoc) CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Cyan,
                                    strokeWidth = 2.dp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF202020),
                                unfocusedContainerColor = Color(0xFF202020),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color.Cyan,
                                unfocusedLabelColor = Color.LightGray
                            )
                        )

                        // Destination field
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = destination,
                                onValueChange = { newValue ->
                                    destination = newValue
                                    destinationCoords = null
                                    Log.d("TEXT_INPUT", "User typed: '$newValue'")
                                },
                                label = { Text("Where to? (Type location)") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    if (isSearching) CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.Cyan,
                                        strokeWidth = 2.dp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF202020),
                                    unfocusedContainerColor = Color(0xFF202020),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color.Cyan,
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )

                            DropdownMenu(
                                expanded = expandedSuggestions,
                                onDismissRequest = {
                                    expandedSuggestions = false
                                    Log.d("DROPDOWN", "Dropdown dismissed")
                                },
                                modifier = Modifier
                                    .background(Color(0xFF272727))
                                    .heightIn(max = 200.dp)
                            ) {
                                if (suggestions.isEmpty() && isSearching) {
                                    DropdownMenuItem(
                                        text = { Text("Searching...", color = Color.Gray) },
                                        onClick = { }
                                    )
                                } else {
                                    suggestions.forEach { place ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = place.display_name,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    maxLines = 2
                                                )
                                            },
                                            onClick = {
                                                destination = place.display_name
                                                destinationCoords = LocationCoords(
                                                    lat = place.lat.toDouble(),
                                                    lng = place.lon.toDouble(),
                                                    name = place.display_name
                                                )
                                                expandedSuggestions = false
                                                Log.d("SELECTION", "Selected: ${place.display_name} at ${place.lat}, ${place.lon}")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Automation status indicator
                        if (automationStatus != "Ready" || usingFallback) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (usingFallback) Color(0xFFFF6B35).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f)
                                )
                            ) {
                                Text(
                                    text = if (usingFallback) "Using offline data - Automation unavailable" else "Status: $automationStatus",
                                    modifier = Modifier.padding(8.dp),
                                    color = if (usingFallback) Color(0xFFFF6B35) else Color(0xFF4CAF50),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Find Ride button
                        Button(
                            enabled = !loadingLoc && locationPermissionGranted &&
                                    currentCoords != null && destinationCoords != null && !loading,
                            onClick = {
                                val pickup = currentCoords
                                val dropoff = destinationCoords
                                if (pickup != null && dropoff != null) {
                                    loading = true
                                    usingFallback = false
                                    automationStatus = "Connecting to automation service..."

                                    Log.d("RIDE_SEARCH", "Searching rides from ${pickup.lat}, ${pickup.lng} to ${dropoff.lat}, ${dropoff.lng}")

                                    coroutineScope.launch {
                                        try {
                                            automationStatus = "Extracting real-time data..."

                                            val automationRequest = RideAutomationService.AutomationRequest(
                                                pickup = pickup,
                                                dropoff = dropoff,
                                                services = listOf("uber", "ola", "rapido")
                                            )

                                            val realTimeResults = automationService.getRealTimeRides(automationRequest)

                                            if (realTimeResults.isNotEmpty()) {
                                                results = realTimeResults
                                                automationStatus = "Real-time data loaded successfully"
                                                usingFallback = false
                                                Log.d("RIDE_SEARCH", "Found ${realTimeResults.size} real-time ride options")
                                            } else {
                                                throw Exception("No real-time data available")
                                            }

                                        } catch (e: Exception) {
                                            Log.e("RIDE_SEARCH", "Automation failed, using fallback: ${e.message}")
                                            automationStatus = "Automation failed - using offline data"
                                            usingFallback = true
                                            results = generateStaticRideData(pickup, dropoff)
                                        } finally {
                                            loading = false
                                            // Reset status after 3 seconds
                                            delay(3000)
                                            if (automationStatus != "Ready") {
                                                automationStatus = "Ready"
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF5DCC06),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val buttonText = when {
                                    !locationPermissionGranted -> "Grant Location Permission"
                                    currentCoords == null -> "Getting Location..."
                                    destinationCoords == null -> "Select Destination"
                                    else -> "Find Rides"
                                }
                                Text(buttonText, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Ride results
                if (results.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(results.sortedBy { it.price }) { index, ride ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { launchRideApp(context, ride.deepLink) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Company logo placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    when (ride.company) {
                                                        "Uber" -> Color(0xFF000000)
                                                        "Ola" -> Color(0xFF00C853)
                                                        "Namma Yatri" -> Color(0xFF2196F3)
                                                        else -> Color(0xFFFF5722)
                                                    },
                                                    RoundedCornerShape(20.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ride.company.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ride.company,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                "${ride.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} • ₹${String.format("%.0f", ride.price)} • ${ride.etaSeconds / 60} min",
                                                color = Color.LightGray,
                                                fontSize = 14.sp
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "TAP TO BOOK",
                                                    color = Color.Cyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (usingFallback) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "• ESTIMATE",
                                                        color = Color.Red,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (currentCoords != null && destinationCoords != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "From",
                                                tint = Color.Green,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Current",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "To",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                destinationCoords!!.name.take(25) + if (destinationCoords!!.name.length > 25) "..." else "",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(2f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Destination",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
