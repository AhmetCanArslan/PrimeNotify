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
import com.arslan.primenotify.ui.ignored.IgnoredNotificationsScreen
import com.arslan.primenotify.ui.logging.LoggingScreen
import com.arslan.primenotify.ui.permissions.PermissionsScreen

sealed class Screen(val route: String, val labelResId: Int) {
    data object Home : Screen("home", com.arslan.primenotify.R.string.nav_home)
    data object Permissions : Screen("permissions", com.arslan.primenotify.R.string.nav_permissions)
    data object Rules : Screen("rules", com.arslan.primenotify.R.string.nav_rules)
    data object AddEditRule : Screen("add_edit_rule/{ruleId}", com.arslan.primenotify.R.string.nav_add_edit_rule) {
        fun createRoute(ruleId: String?) = "add_edit_rule/${ruleId ?: "new"}"
    }
    data object CreatePattern : Screen("create_pattern", com.arslan.primenotify.R.string.nav_create_pattern)
    data object Logging : Screen("logging", com.arslan.primenotify.R.string.nav_logging)
    data object IgnoredNotifications : Screen("ignored_notifications", com.arslan.primenotify.R.string.nav_ignored_notifications)
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
            route = Screen.Rules.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) }
        ) {
            com.arslan.primenotify.ui.settings.RulesScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                },
                onNavigateToAddEditRule = { ruleId ->
                    rootNavController.navigate(Screen.AddEditRule.createRoute(ruleId))
                }
            )
        }
        composable(
            route = Screen.AddEditRule.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId")
            com.arslan.primenotify.ui.settings.AddEditRuleScreen(
                ruleId = ruleId,
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                },
                onNavigateToCreatePattern = {
                    rootNavController.navigate(Screen.CreatePattern.route)
                }
            )
        }
        composable(
            route = Screen.CreatePattern.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) }
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
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) }
        ) {
            LoggingScreen(
                onNavigateBack = {
                    if (rootNavController.previousBackStackEntry != null) {
                        rootNavController.popBackStack()
                    }
                },
                onNavigateToIgnored = {
                    rootNavController.navigate(Screen.IgnoredNotifications.route)
                },
                onNavigateToAddEditRule = {
                    rootNavController.navigate(Screen.AddEditRule.createRoute(null))
                }
            )
        }
        composable(
            route = Screen.IgnoredNotifications.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)) }
        ) {
            IgnoredNotificationsScreen(
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
                    onNavigateToRules = { rootNavController.navigate(Screen.Rules.route) }
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
