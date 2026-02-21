package com.arslan.primenotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arslan.primenotify.data.AppListManager
import com.arslan.primenotify.data.RulesManager
import com.arslan.primenotify.navigation.AppNavigation
import com.arslan.primenotify.ui.theme.PrimeNotifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the proximity sensor check at app launch
        RulesManager(this).hasProximitySensor()

        // Initialize app list and icons
        AppListManager.initialize(this)

        setContent {
            PrimeNotifyTheme {
                AppNavigation()
            }
        }
    }
}