package com.example.jetpack_test1

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Notification data model
data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    RIDE_CONFIRMED, DRIVER_UPDATE, PAYMENT, PROMOTION, GENERAL
}

@Composable
fun NotificationScreen() {
    // Hardcoded notifications API
    val notifications = listOf(
        NotificationItem(
            1,
            "Ride Confirmed",
            "Your Uber ride has been confirmed. Driver: John Smith, ETA: 5 mins",
            "2 mins ago",
            NotificationType.RIDE_CONFIRMED
        ),
        NotificationItem(
            2,
            "Driver Arriving",
            "Your Ola driver is 1 minute away. Please be ready at the pickup location.",
            "5 mins ago",
            NotificationType.DRIVER_UPDATE
        ),
        NotificationItem(
            3,
            "Payment Successful",
            "Payment of ₹245.50 has been processed successfully for your trip.",
            "1 hour ago",
            NotificationType.PAYMENT,
            true
        ),
        NotificationItem(
            4,
            "Special Offer!",
            "Get 20% off on your next 3 rides. Use code: SAVE20. Valid till Aug 31.",
            "3 hours ago",
            NotificationType.PROMOTION
        ),
        NotificationItem(
            5,
            "Trip Completed",
            "Your trip from MG Road to Koramangala has been completed. Rate your driver!",
            "1 day ago",
            NotificationType.GENERAL,
            true
        ),
        NotificationItem(
            6,
            "Namma Yatri Update",
            "Your booking has been assigned to driver Rajesh. Vehicle: KA01AB1234",
            "2 days ago",
            NotificationType.DRIVER_UPDATE,
            true
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
                "Notifications",
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
                itemsIndexed(notifications) { index, notification ->
                    AnimatedNotificationCard(
                        notification = notification,
                        index = index
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedNotificationCard(notification: NotificationItem, index: Int) {
    var animationPlayed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "notificationScale"
    )

    LaunchedEffect(Unit) {
        delay((index * 150).toLong()) // Staggered animation with 150ms delay
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
                .background(
                    if (notification.isRead) Color(0xFF1A1A1A) else Color(0xFF222222)
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (notification.isRead) Color(0xFF1A1A1A) else Color(0xFF222222)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Notification icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            getNotificationColor(notification.type),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            notification.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            notification.timestamp,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        notification.message,
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        lineHeight = 20.sp
                    )
                }

                // Unread indicator
                if (!notification.isRead) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Cyan, CircleShape)
                    )
                }
            }
        }
    }
}

fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.RIDE_CONFIRMED -> Icons.Default.DirectionsCar
        NotificationType.DRIVER_UPDATE -> Icons.Default.Person
        NotificationType.PAYMENT -> Icons.Default.Payment
        NotificationType.PROMOTION -> Icons.Default.LocalOffer
        NotificationType.GENERAL -> Icons.Default.Notifications
    }
}

fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.RIDE_CONFIRMED -> Color(0xFF4CAF50)
        NotificationType.DRIVER_UPDATE -> Color(0xFF2196F3)
        NotificationType.PAYMENT -> Color(0xFF9C27B0)
        NotificationType.PROMOTION -> Color(0xFFFF9800)
        NotificationType.GENERAL -> Color(0xFF607D8B)
    }
}
