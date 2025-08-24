package com.example.jetpack_test1

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TaxiAlert
import androidx.compose.material3.ButtonDefaults.elevatedButtonColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
    val etaSeconds: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp


    var containerMovedUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<RideOption>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }

    // Add state variables for text inputs
    var currentLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }

    // Hardcoded ride options
    val rideData = listOf(

        RideOption("Uber", "ic_uber", "sedan", 140.0, 80),
        RideOption("Ola", "ic_ola", "auto", 125.0, 120),
        RideOption("Namma Yatri", "ic_ny", "suv", 160.0, 150),
        RideOption("Rapid", "ic_rapid", "sedan", 350.0, 90),

        RideOption("Uber", "ic_uber", "sedan", 400.0, 100),
        RideOption("Ola", "ic_ola", "sedan", 250.0, 100),


    )

    val filteredResults = results.filter {
        selectedFilter == "All" || it.type.equals(selectedFilter, ignoreCase = true)
    }.sortedBy { it.price }

    // Animate container position from bottom to top - made initial position higher
    val containerOffsetY by animateDpAsState(
        targetValue = if (containerMovedUp) 16.dp else screenHeight - 450.dp, // Changed from 350.dp to 450.dp
        animationSpec = tween(durationMillis = 800),
        label = "containerOffsetY"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212 )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()

        ) {
            // Conditionally show background image or solid black
            if (!containerMovedUp) {
                // Background Image - only show when container is at bottom
                Image(
                    painter = painterResource(R.drawable.maps),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize().blur(2.dp)
                )
            } else {
                // Solid black background when container is moved up
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            // Main container that moves as one unit
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = containerOffsetY)
                    .padding(horizontal = 16.dp)
            ) {
                // Input Container
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

                                // Simulate loading data after delay
                                coroutineScope.launch {
                                    delay(1000)
                                    loading = false
                                    results = rideData
                                }
                            },
                            colors = elevatedButtonColors(
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

                // Results area that moves with the container
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
                        // Filter Options Row on top
                        item {
                            FilterRow(
                                filters = listOf("All", "Sedan", "Auto", "SUV"),
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it }
                            )
                        }

                        // Result List with bubble pop animation
                        itemsIndexed(filteredResults) { index, ride ->
                            BubblePopRideCard(
                                ride = ride,
                                index = index,
                                isVisible = containerMovedUp && !loading
                            )
                        }
                    }
                }
            }

            // Loading indicator
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
    isVisible: Boolean
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
            delay((index * 100).toLong()) // Staggered animation
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp))
                .background(Color(0xFF222222)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Real company logos instead of icons
                Image(
                    painter = painterResource(
                        when (ride.company.lowercase()) {
                            "uber" -> R.drawable.uber
                            "ola" -> R.drawable.ola
                            "namma yatri" -> R.drawable.namma
                            "rapid" -> R.drawable.rapido
                            else -> R.drawable.uber // fallback
                        }
                    ),
                    contentDescription = "${ride.company} logo",
                    modifier = Modifier.size(40.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        ride.company,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        "${ride.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} - \$${String.format("%.2f", ride.price)} - ETA: ${ride.etaSeconds / 60} min",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
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
                ),
                modifier = Modifier.shadow(
                    10.dp,
                    RoundedCornerShape(12.dp),
                    clip = false
                )
            )
        }
    }
}
