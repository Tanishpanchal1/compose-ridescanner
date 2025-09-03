package com.example.jetpack_test1

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.compose.NavHost

@Composable
fun AppEntryPoint() {
    val navController = rememberNavController()
    val tabs = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("History", Icons.Default.AccessTime, "history"),
        BottomNavItem("Notifications", Icons.Default.Notifications, "notifications"),
        BottomNavItem("Profile", Icons.Default.Person, "profile")
    )

    Scaffold(
        bottomBar = { BottomNavBar(navController, tabs) }
    ) { paddingValues ->

        NavHost(
            navController,
            startDestination = "home",
            Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("history") { HistoryScreen() } // Updated
            composable("notifications") { NotificationScreen() } // Updated
            composable("profile") { SimpleScreen("Profile") }
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun BottomNavBar(navController: NavHostController, items: List<BottomNavItem>) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF151418)
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                alwaysShowLabel = false,
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00FFFF),
                    unselectedIconColor = Color.Gray,
                    indicatorColor = if (selected) Color(0xFF5DCC06) else Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SimpleScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}