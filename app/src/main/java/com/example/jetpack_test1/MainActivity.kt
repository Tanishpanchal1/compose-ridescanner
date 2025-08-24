package com.example.jetpack_test1

import android.os.Bundle
//
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.jetpack_test1.ui.theme.Jetpack_test1Theme
//
//
////import androidx.compose.runtime.Composable
//
//import androidx.compose.runtime.getValue
//
//import androidx.compose.runtime.mutableStateOf
//
//import androidx.compose.runtime.remember
//
//import androidx.compose.runtime.setValue
//
//
//
//import androidx.compose.foundation.layout.Column
//
//import androidx.compose.foundation.layout.Row
//
//import androidx.compose.foundation.layout.Spacer
//
//import androidx.compose.foundation.layout.height
//
//import androidx.compose.foundation.layout.padding
//
//import androidx.compose.foundation.layout.width
//
//import androidx.compose.foundation.lazy.LazyColumn
//
//import androidx.compose.foundation.lazy.items
//
//
//
//import androidx.compose.material3.Button
//
//import androidx.compose.material3.Divider
//
//import androidx.compose.material3.ListItem
//
//import androidx.compose.material3.MaterialTheme
//
//import androidx.compose.material3.Text
//
//import androidx.compose.material3.TextButton
//
//import androidx.compose.material3.TextField
//
//
//
//import androidx.compose.ui.Alignment
//
////import androidx.compose.ui.Modifier
//
//import androidx.compose.ui.unit.dp

//package com.example.jetpack_test1  // Your actual package name

import android.net.Uri
//import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView





import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.*


import androidx.navigation.compose.*
import com.example.jetpack_test1.ui.theme.RideScannerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RideScannerTheme {
                Splash()
            }
        }
    }
}
