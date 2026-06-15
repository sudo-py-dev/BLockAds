package com.blockads.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.blockads.app.i18n.LocalStrings
import com.blockads.app.ui.about.AboutScreen
import com.blockads.app.ui.home.HomeScreen
import com.blockads.app.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    padding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = Modifier.padding(padding),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = tween(400),
            ) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -300 },
                animationSpec = tween(400),
            ) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = tween(400),
            ) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 300 },
                animationSpec = tween(400),
            ) + fadeOut(animationSpec = tween(400))
        },
    ) {
        composable<Home> { HomeScreen() }
        composable<Settings> { SettingsScreen() }
        composable<About> { AboutScreen() }
    }
}

@Composable
fun AppBottomBar(navController: NavHostController) {
    val strings = LocalStrings.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    NavigationBar {
        NavigationBarItem(
            selected = currentDest?.hasRoute<Home>() == true,
            onClick = { navController.navigate(Home) { launchSingleTop = true } },
            icon = { Icon(Icons.Rounded.Home, contentDescription = strings.navHome) },
            label = { Text(strings.navHome) },
        )
        NavigationBarItem(
            selected = currentDest?.hasRoute<Settings>() == true,
            onClick = { navController.navigate(Settings) { launchSingleTop = true } },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = strings.navSettings) },
            label = { Text(strings.navSettings) },
        )
        NavigationBarItem(
            selected = currentDest?.hasRoute<About>() == true,
            onClick = { navController.navigate(About) { launchSingleTop = true } },
            icon = { Icon(Icons.Rounded.Info, contentDescription = strings.navAbout) },
            label = { Text(strings.navAbout) },
        )
    }
}
