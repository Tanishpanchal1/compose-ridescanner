package com.example.jetpack_test1

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

data class AppInstallStatus(
    val appName: String,
    val displayName: String,
    val packageName: String,
    val isInstalled: Boolean,
    val color: Long,
    val description: String
)

// ... (keep existing functions fetchOpenStreetMapSuggestions, getCurrentLocation, generateStaticRideData, launchRideApp)

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

/**
 * Function to open Play Store for app installation
 */
fun openPlayStore(context: Context, packageName: String) {
    try {
        // Try to open Play Store app directly
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$packageName")
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
        Log.d("PLAY_STORE", "Opened Play Store app for: $packageName")
    } catch (e: ActivityNotFoundException) {
        // Fallback to web browser if Play Store app is not available
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(intent)
            Log.d("PLAY_STORE", "Opened Play Store web for: $packageName")
        } catch (e2: Exception) {
            Log.e("PLAY_STORE", "Failed to open Play Store: ${e2.message}")
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize real-time automation service
    val realTimeService = remember { RealTimeAutomationService(context) }

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

    // App installation state
    var appInstallationStatus by remember { mutableStateOf<List<AppInstallStatus>>(emptyList()) }
    var showAppCards by remember { mutableStateOf(true) }

    // Automation status
    var automationStatus by remember { mutableStateOf("Ready") }
    var usingFallback by remember { mutableStateOf(false) }
    var accessibilityRequired by remember { mutableStateOf(false) }

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

    // Check app installation status
    LaunchedEffect(Unit) {
        val supportedApps = realTimeService.getSupportedApps()
        val installationStatus = supportedApps.map { (key, config) ->
            val isInstalled = realTimeService.getAppInstallationStatus()[key] ?: false
            AppInstallStatus(
                appName = key,
                displayName = config.displayName,
                packageName = config.packageName,
                isInstalled = isInstalled,
                color = config.color,
                description = when (key) {
                    "uber" -> "Global ride-hailing service"
                    "ola" -> "India's leading ride-sharing app"
                    "rapido" -> "Bike taxi & auto rides"
                    "nammayatri" -> "Zero commission ride app"
                    "blusmart" -> "Electric cab service (Suspended)"
                    else -> "Ride-hailing service"
                }
            )
        }
        appInstallationStatus = installationStatus
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
                // App Installation Status Cards
                if (showAppCards && appInstallationStatus.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF272727)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📱 Ride Apps Status",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = { showAppCards = false }
                                ) {
                                    Text("Hide", color = Color(0xFF888888), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(appInstallationStatus) { appStatus ->
                                    AppStatusCard(
                                        appStatus = appStatus,
                                        onInstallClick = { packageName ->
                                            openPlayStore(context, packageName)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val installedCount = appInstallationStatus.count { it.isInstalled }
                            val totalCount = appInstallationStatus.size

                            Text(
                                text = "✅ $installedCount/$totalCount apps installed • Tap missing apps to download",
                                color = Color(0xFF888888),
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

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
                        if (automationStatus != "Ready" || usingFallback || accessibilityRequired) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        accessibilityRequired -> Color(0xFFE91E63).copy(alpha = 0.2f)
                                        usingFallback -> Color(0xFFFF6B35).copy(alpha = 0.2f)
                                        else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            accessibilityRequired -> "⚠️ Accessibility Service Required - Tap to Enable"
                                            usingFallback -> "📱 Using offline estimates - Live data unavailable"
                                            else -> "Status: $automationStatus"
                                        },
                                        color = when {
                                            accessibilityRequired -> Color(0xFFE91E63)
                                            usingFallback -> Color(0xFFFF6B35)
                                            else -> Color(0xFF4CAF50)
                                        },
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
                                    automationStatus = "Getting smart price estimates..."
                                    coroutineScope.launch {
                                        try {
                                            // Phase 1: Get instant smart estimates
                                            val estimates = realTimeService.getRealTimeRides(pickup, dropoff)
                                            results = estimates
                                            automationStatus = "✅ Smart estimates ready!"
                                            delay(1500)

                                            // Phase 2: Open apps with deep links
                                            automationStatus = "🔗 Opening apps with your route..."
                                            realTimeService.openAppsWithDeepLinks(
                                                pickup = pickup,
                                                dropoff = dropoff,
                                                onProgress = { status ->
                                                    automationStatus = status
                                                },
                                                onComplete = {
                                                    automationStatus = "Ready for next search"
                                                }
                                            )
                                        } catch (e: Exception) {
                                            Log.e("RIDE_SEARCH", "Error: ${e.message}")
                                            automationStatus = "Using fallback estimates"
                                            results = generateStaticRideData(pickup, dropoff)
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF5DCC06),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Opening Apps...", fontSize = 16.sp)
                            } else {
                                val buttonText = when {
                                    !locationPermissionGranted -> "Grant Location Permission"
                                    currentCoords == null -> "Getting Location..."
                                    destinationCoords == null -> "Select Destination"
                                    else -> "Find Rides"
                                }
                                Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Info text
                        if (!loading && results.isEmpty() && currentCoords != null && destinationCoords != null) {
                            Text(
                                text = "💡 Get smart estimates + open installed ride apps with your exact route",
                                color = Color(0xFF888888),
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
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
                                                        "Rapido" -> Color(0xFFFF5722)
                                                        else -> Color(0xFF666666)
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
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "TAP TO BOOK",
                                                color = Color.Cyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                if (usingFallback) "ESTIMATE" else "LIVE PRICE",
                                                color = if (usingFallback) Color(0xFFFFA500) else Color(0xFF4CAF50),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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

@Composable
fun AppStatusCard(
    appStatus: AppInstallStatus,
    onInstallClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appStatus.isInstalled)
                Color(appStatus.color).copy(alpha = 0.15f)
            else
                Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color(appStatus.color),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appStatus.displayName.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // App name
            Text(
                text = appStatus.displayName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // Status and action
            if (appStatus.isInstalled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Installed",
                        color = Color(0xFF4CAF50),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = { onInstallClick(appStatus.packageName) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5DCC06),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Install",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
