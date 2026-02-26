package com.arslan.primenotify.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arslan.primenotify.R
import com.arslan.primenotify.ui.home.HomeScreen
import com.arslan.primenotify.ui.logging.LoggingScreen
import com.arslan.primenotify.ui.permissions.PermissionsScreen

sealed class Screen(val route: String, val labelResId: Int) {
    data object Home : Screen("home", com.arslan.primenotify.R.string.nav_home)
    data object Permissions : Screen("permissions", com.arslan.primenotify.R.string.nav_permissions)
    data object FlashSettings : Screen("flash_settings", com.arslan.primenotify.R.string.nav_flash_settings)
    data object WakeUpScreenSettings : Screen("wake_up_screen_settings", com.arslan.primenotify.R.string.nav_wake_up_screen_settings)
    data object AODSettings : Screen("aod_settings", com.arslan.primenotify.R.string.nav_aod_settings)
    data object CreatePattern : Screen("create_pattern", com.arslan.primenotify.R.string.nav_create_pattern)
    data object AddEditFlashRule : Screen("add_edit_flash_rule/{ruleId}", com.arslan.primenotify.R.string.nav_add_edit_flash_rule) {
        fun createRoute(ruleId: String?) = "add_edit_flash_rule/${ruleId ?: "new"}"
    }
    data object AddEditWakeUpRule : Screen("add_edit_wake_up_rule/{ruleId}", com.arslan.primenotify.R.string.nav_add_edit_wake_up_rule) {
        fun createRoute(ruleId: String?) = "add_edit_wake_up_rule/${ruleId ?: "new"}"
    }
    data object AddEditAodRule : Screen("add_edit_aod_rule/{ruleId}", com.arslan.primenotify.R.string.nav_add_edit_aod_rule) {
        fun createRoute(ruleId: String?) = "add_edit_aod_rule/${ruleId ?: "new"}"
    }
    data object Logging : Screen("logging", com.arslan.primenotify.R.string.nav_logging)
}

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = "main_flow",
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        composable(
            route = "main_flow",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            MainFlowScreen(rootNavController)
        }

        composable(
            route = Screen.FlashSettings.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.arslan.primenotify.ui.settings.FlashSettingsScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                },
                onNavigateToAddEditRule = { ruleId ->
                    rootNavController.navigate(Screen.AddEditFlashRule.createRoute(ruleId))
                },
                onNavigateToCreatePattern = {
                    rootNavController.navigate(Screen.CreatePattern.route)
                }
            )
        }
        composable(
            route = Screen.AddEditFlashRule.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId")
            com.arslan.primenotify.ui.settings.AddEditFlashRuleScreen(
                ruleId = ruleId,
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = Screen.WakeUpScreenSettings.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.arslan.primenotify.ui.settings.WakeUpScreenSettingsScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                },
                onNavigateToAddEditRule = { ruleId ->
                    rootNavController.navigate(Screen.AddEditWakeUpRule.createRoute(ruleId))
                }
            )
        }
        composable(
            route = Screen.AddEditWakeUpRule.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId")
            com.arslan.primenotify.ui.settings.AddEditWakeUpRuleScreen(
                ruleId = ruleId,
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = Screen.AODSettings.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.arslan.primenotify.ui.settings.AODSettingsScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                },
                onNavigateToAddEditRule = { ruleId ->
                    rootNavController.navigate(Screen.AddEditAodRule.createRoute(ruleId))
                }
            )
        }
        composable(
            route = Screen.AddEditAodRule.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId")
            com.arslan.primenotify.ui.settings.AddEditAodRuleScreen(
                ruleId = ruleId,
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = Screen.CreatePattern.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.arslan.primenotify.ui.settings.CreatePatternScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = Screen.Logging.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) }
        ) {
            LoggingScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFlowScreen(rootNavController: NavController) {
    val bottomNavController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { rootNavController.navigate(Screen.Logging.route) }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.nav_logging)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val currentBackStackEntry = bottomNavController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry.value?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.cd_settings)) },
                    label = { Text(stringResource(R.string.nav_home)) },
                    selected = currentRoute == Screen.Home.route,
                    onClick = {
                        bottomNavController.navigate(Screen.Home.route) {
                            popUpTo(bottomNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_permissions)) },
                    label = { Text(stringResource(R.string.nav_permissions)) },
                    selected = currentRoute == Screen.Permissions.route,
                    onClick = {
                        bottomNavController.navigate(Screen.Permissions.route) {
                            popUpTo(bottomNavController.graph.startDestinationId) {
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
            navController = bottomNavController,
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
                HomeScreen(
                    onNavigateToFlashSettings = { rootNavController.navigate(Screen.FlashSettings.route) },
                    onNavigateToWakeUpScreenSettings = { rootNavController.navigate(Screen.WakeUpScreenSettings.route) },
                    onNavigateToAODSettings = { rootNavController.navigate(Screen.AODSettings.route) }
                )
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
