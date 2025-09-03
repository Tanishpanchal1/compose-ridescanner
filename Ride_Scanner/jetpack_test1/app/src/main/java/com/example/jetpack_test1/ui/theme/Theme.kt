package com.example.jetpack_test1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FFFF),
    onPrimary = Color.Black,
    secondary = Color(0xFF7FFFD4),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF181818),
    onSurface = Color(0xFFFFFFFF),
)

@Composable
fun RideScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonDarkColorScheme,
        typography = Typography(),
        content = content
    )
}