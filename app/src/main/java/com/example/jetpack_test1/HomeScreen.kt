package com.example.jetpack_test1

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RideOption(
    val company: String,
    val logoUrl: String,
    val type: String,
    val price: Double,
    val etaSeconds: Int,
    val deepLink: String = ""
)

data class LocationCoords(val lat: Double, val lng: Double, val name: String)

@Composable
fun HomeScreen(navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val context = LocalContext.current

    var containerMovedUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<RideOption>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var currentLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }

    val locationCoords = mapOf(
        "whitefield" to LocationCoords(12.9698, 77.7500, "Whitefield"),
        "kormangala" to LocationCoords(12.9352, 77.6245, "Kormangala"),
        "itpl" to LocationCoords(12.9850, 77.7360, "ITPL"),
        "kr puram" to LocationCoords(12.9926, 77.7569, "KR Puram"),
        "yelahanka" to LocationCoords(13.1007, 77.5963, "Yelahanka"),
        "rajajinagar" to LocationCoords(12.9915, 77.5554, "Rajajinagar")
    )

    fun getLocationCoords(input: String): LocationCoords? {
        val inputKey = input.lowercase().trim()
        return locationCoords.entries.find {
            inputKey.contains(it.key)
        }?.value
    }

    fun buildUberLink(pickup: LocationCoords, dropoff: LocationCoords): String {
        return "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=${pickup.lat}&pickup[longitude]=${pickup.lng}&dropoff[latitude]=${dropoff.lat}&dropoff[longitude]=${dropoff.lng}&dropoff[nickname]=${dropoff.name}"
    }

    fun buildOlaLink(pickup: LocationCoords, dropoff: LocationCoords): String {
        return "https://olacabs.com/?pickup=${pickup.lat},${pickup.lng}&dropoff=${dropoff.lat},${dropoff.lng}"
    }

    fun generateRideData(from: String, to: String): List<RideOption> {
        val fromCoords = getLocationCoords(from)
        val toCoords = getLocationCoords(to)

        return listOf(
            RideOption(
                company = "Uber",
                logoUrl = "ic_uber",
                type = "sedan",
                price = 180.0,
                etaSeconds = 45,
                deepLink = if (fromCoords != null && toCoords != null)
                    buildUberLink(fromCoords, toCoords) else "https://m.uber.com/ul/"
            ),
            RideOption(
                company = "Ola",
                logoUrl = "ic_ola",
                type = "auto",
                price = 220.0,
                etaSeconds = 50,
                deepLink = if (fromCoords != null && toCoords != null)
                    buildOlaLink(fromCoords, toCoords) else "https://olacabs.com/"
            ),
            RideOption(
                company = "Namma Yatri",
                logoUrl = "ic_ny",
                type = "sedan",
                price = 260.0,
                etaSeconds = 48,
                deepLink = "https://www.namayatri.com/"
            ),
            RideOption(
                company = "Rapido",
                logoUrl = "ic_rapid",
                type = "auto",
                price = 190.0,
                etaSeconds = 52,
                deepLink = "https://rapido.bike/"
            )
        )
    }

    fun launchRideApp(context: Context, deepLink: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error
        }
    }

    val filteredResults = results.filter {
        selectedFilter == "All" || it.type.equals(selectedFilter, ignoreCase = true)
    }.sortedBy { it.price }

    val containerOffsetY by animateDpAsState(
        targetValue = if (containerMovedUp) 16.dp else screenHeight - 450.dp,
        animationSpec = tween(durationMillis = 800),
        label = "containerOffsetY"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!containerMovedUp) {
                Image(
                    painter = painterResource(R.drawable.maps),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize().blur(2.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = containerOffsetY)
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(16.dp))
                        .background(
                            Color(0xFF272727),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = currentLocation,
                            onValueChange = { currentLocation = it },
                            label = { Text("Current Location") },
                            modifier = Modifier.fillMaxWidth(),
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

                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("Destination") },
                            modifier = Modifier.fillMaxWidth(),
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

                        Button(
                            onClick = {
                                containerMovedUp = true
                                loading = true
                                coroutineScope.launch {
                                    delay(1000)
                                    results = generateRideData(currentLocation, destination)
                                    loading = false
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
                            Text("Find Ride", fontSize = 18.sp)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = containerMovedUp && !loading && results.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 400)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterRow(
                                filters = listOf("All", "Sedan", "Auto", "SUV"),
                                selectedFilter = selectedFilter,
                                onFilterSelected = { filter -> selectedFilter = filter }
                            )
                        }

                        itemsIndexed(filteredResults) { index, ride ->
                            BubblePopRideCard(
                                ride = ride,
                                index = index,
                                isVisible = containerMovedUp && !loading,
                                fromLocation = currentLocation,
                                toLocation = destination,
                                onClick = {
                                    launchRideApp(context, ride.deepLink)
                                }
                            )
                        }
                    }
                }
            }

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF00FFFF)
                )
            }
        }
    }
}

@Composable
fun BubblePopRideCard(
    ride: RideOption,
    index: Int,
    isVisible: Boolean,
    fromLocation: String,
    toLocation: String,
    onClick: () -> Unit
) {
    var animationPlayed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubbleScale"
    )

    LaunchedEffect(isVisible) {
        if (isVisible && !animationPlayed) {
            delay((index * 100).toLong())
            animationPlayed = true
        }
    }

    AnimatedVisibility(
        visible = scale > 0f,
        enter = scaleIn(
            initialScale = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            when (ride.company.lowercase()) {
                                "uber" -> R.drawable.uber
                                "ola" -> R.drawable.ola
                                "namma yatri" -> R.drawable.namma
                                "rapido" -> R.drawable.rapido
                                else -> R.drawable.uber
                            }
                        ),
                        contentDescription = "${ride.company} logo",
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ride.company,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            "${ride.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} - ₹${String.format("%.0f", ride.price)} - ETA: ${ride.etaSeconds / 60} min",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        "TAP TO BOOK",
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (fromLocation.isNotEmpty() && toLocation.isNotEmpty()) {
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
                            fromLocation,
                            color = Color.White,
                            fontSize = 14.sp,
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
                            toLocation,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
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

@Composable
fun FilterRow(filters: List<String>, selectedFilter: String, onFilterSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(filter, color = if (selectedFilter == filter) Color.Black else Color.LightGray)
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selectedFilter == filter) Color(0xFF00FFFF) else Color(0xFF333333)
                )
            )
        }
    }
}
