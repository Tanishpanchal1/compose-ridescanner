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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Journey data model
data class JourneyItem(
    val id: Int,
    val fromLocation: String,
    val toLocation: String,
    val date: String,
    val time: String,
    val price: Double,
    val company: String,
    val vehicleType: String,
    val duration: String,
    val distance: String,
    val status: JourneyStatus
)

enum class JourneyStatus {
    COMPLETED, CANCELLED, ONGOING
}

@Composable
fun HistoryScreen() {
    // Hardcoded journey history API
    val journeys = listOf(
        JourneyItem(
            1,
            "MG Road Metro Station",
            "Koramangala 5th Block",
            "Aug 21, 2025",
            "09:15 AM",
            245.50,
            "Uber",
            "Sedan",
            "25 mins",
            "8.5 km",
            JourneyStatus.COMPLETED
        ),
        JourneyItem(
            2,
            "Electronic City",
            "Whitefield",
            "Aug 20, 2025",
            "06:30 PM",
            420.00,
            "Ola",
            "SUV",
            "45 mins",
            "32 km",
            JourneyStatus.COMPLETED
        ),
        JourneyItem(
            3,
            "Banashankari",
            "Indiranagar",
            "Aug 19, 2025",
            "02:20 PM",
            180.75,
            "Namma Yatri",
            "Auto",
            "35 mins",
            "12 km",
            JourneyStatus.COMPLETED
        ),
        JourneyItem(
            4,
            "Brigade Road",
            "Airport",
            "Aug 18, 2025",
            "05:45 AM",
            650.00,
            "Rapid",
            "Sedan",
            "55 mins",
            "42 km",
            JourneyStatus.COMPLETED
        ),
        JourneyItem(
            5,
            "Jayanagar",
            "Commercial Street",
            "Aug 17, 2025",
            "11:30 AM",
            0.0,
            "Uber",
            "Auto",
            "0 mins",
            "0 km",
            JourneyStatus.CANCELLED
        ),
        JourneyItem(
            6,
            "HSR Layout",
            "Marathahalli",
            "Aug 16, 2025",
            "07:10 PM",
            195.25,
            "Ola",
            "Sedan",
            "28 mins",
            "14 km",
            JourneyStatus.COMPLETED
        ),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(
                "Trip History",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(24.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(journeys) { index, journey ->
                    AnimatedJourneyCard(
                        journey = journey,
                        index = index
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedJourneyCard(journey: JourneyItem, index: Int) {
    var animationPlayed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "journeyScale"
    )

    LaunchedEffect(Unit) {
        delay((index * 120).toLong()) // Staggered animation with 120ms delay
        animationPlayed = true
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
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(Color(0xFF222222)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Company logo and status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(
                                when (journey.company.lowercase()) {
                                    "uber" -> R.drawable.uber
                                    "ola" -> R.drawable.ola
                                    "namma yatri" -> R.drawable.namma
                                    "rapid" -> R.drawable.rapido
                                    else -> R.drawable.uber
                                }
                            ),
                            contentDescription = "${journey.company} logo",
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getStatusColor(journey.status)
                        ) {
                            Text(
                                journey.status.name,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            journey.date,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            journey.time,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Route information
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            journey.fromLocation,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Dotted line
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.Gray)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            journey.toLocation,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Trip details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Vehicle",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            journey.vehicleType,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            "Duration",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            journey.duration,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            "Distance",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            journey.distance,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Fare",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            if (journey.status == JourneyStatus.CANCELLED) "₹0.00"
                            else "₹${String.format("%.2f", journey.price)}",
                            fontSize = 14.sp,
                            color = if (journey.status == JourneyStatus.CANCELLED) Color.Gray else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun getStatusColor(status: JourneyStatus): Color {
    return when (status) {
        JourneyStatus.COMPLETED -> Color(0xFF4CAF50)
        JourneyStatus.CANCELLED -> Color(0xFFF44336)
        JourneyStatus.ONGOING -> Color(0xFF2196F3)
    }
}
