package com.arslan.primenotify.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arslan.primenotify.service.isPrimeNotifyServiceEnabled
import com.arslan.primenotify.service.setPrimeNotifyServiceEnabled
import com.arslan.primenotify.ui.home.HomeScreen
import com.arslan.primenotify.ui.permissions.PermissionsScreen

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "Home")
    data object Permissions : Screen("permissions", "Permissions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            val permissionItems = com.arslan.primenotify.ui.home.buildPermissionItems(context)
            val allPermissionsGranted = permissionItems.all { it.granted }
            val primeNotifyServiceEnabled = isPrimeNotifyServiceEnabled(context)
            
            TopAppBar(
                title = {
                    Text(
                        text = "PrimeNotify",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                actions = {
                    Switch(
                        checked = primeNotifyServiceEnabled,
                        onCheckedChange = {
                            if (!allPermissionsGranted && it) return@Switch
                            setPrimeNotifyServiceEnabled(context, it)
                        },
                        enabled = allPermissionsGranted || primeNotifyServiceEnabled,
                        modifier = Modifier.padding(end = 32.dp)
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val currentBackStackEntry = navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry.value?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == Screen.Home.route,
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Permissions") },
                    label = { Text("Permissions") },
                    selected = currentRoute == Screen.Permissions.route,
                    onClick = {
                        navController.navigate(Screen.Permissions.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding).padding(top = 8.dp)
        ) {
            composable(
                route = Screen.Home.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
            ) {
                HomeScreen()
            }
            composable(
                route = Screen.Permissions.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                PermissionsScreen()
            }
        }
    }
}
