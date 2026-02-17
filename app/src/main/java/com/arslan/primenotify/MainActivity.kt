package com.arslan.primenotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.arslan.primenotify.ui.home.HomeScreen
import com.arslan.primenotify.ui.theme.PrimeNotifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrimeNotifyTheme {
                HomeScreen(modifier = Modifier)
            }
        }
    }
}