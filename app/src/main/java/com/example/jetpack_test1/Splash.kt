package com.example.jetpack_test1

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlinx.coroutines.delay



@Composable
fun Splash() { // Entry point for your app
    val navController = rememberNavController()
    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("landing") { LandingScreen(navController) }
        composable("next") { NextScreen() }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("landing") { popUpTo("splash") { inclusive = true } }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.splashscreen), // Add your image to drawable
            contentDescription = "Splash",
            modifier = Modifier.fillMaxSize() ,
            contentScale = ContentScale.Crop  // Use Crop as 'Cover' does not exist
        )
    }
}

@Composable
fun LandingScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.maps), // Add your image to drawable
            contentDescription = "Landing Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Use Crop here too
        )
        FindRideButton(onClick = { navController.navigate("next") })
    }
}

@Composable
fun FindRideButton(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val modifier = Modifier.background(color = Color(0xFF222222)).padding(10.dp) ;
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.2f else 1f,
        animationSpec = keyframes {
            durationMillis = 600
            1.2f at 100
            0.8f at 200

        },
        finishedListener = { if (pressed) { pressed = false; onClick() } }
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = { pressed = true },
            modifier = Modifier
                .scale(scale)
                .padding(16.dp)
        ) {
            Text("Find Ride")
        }
    }
}

@Composable
fun NextScreen() {
    AppEntryPoint()
}


