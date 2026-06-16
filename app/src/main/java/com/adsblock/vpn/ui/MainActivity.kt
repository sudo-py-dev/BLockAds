package com.adsblock.vpn.ui

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adsblock.vpn.R
import com.adsblock.vpn.data.SettingsRepository
import com.adsblock.vpn.service.AdsBlockVpnService
import com.adsblock.vpn.service.VpnStateManager
import com.adsblock.vpn.ui.screens.AboutScreen
import com.adsblock.vpn.ui.screens.HomeScreen
import com.adsblock.vpn.ui.screens.LogsScreen
import com.adsblock.vpn.ui.screens.SettingsScreen
import com.adsblock.vpn.ui.theme.AdsBlockTheme

class MainActivity : ComponentActivity() {
    private val vpnRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intent =
                    Intent(this, AdsBlockVpnService::class.java).apply {
                        action = AdsBlockVpnService.ACTION_START
                    }
                startService(intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val themePreference by settingsRepository.theme.collectAsState(initial = null)
            val languagePreference by settingsRepository.language.collectAsState(initial = null)

            if (themePreference == null || languagePreference == null) {
                return@setContent
            }

            val isDarkTheme =
                when (themePreference) {
                    "dark", stringResource(R.string.theme_dark), "Dark" -> true
                    "light", stringResource(R.string.theme_light), "Light" -> false
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }

            val languageCode =
                when (languagePreference) {
                    "en", stringResource(R.string.language_en), "English", "אנגלית", "Английский", "Anglais" -> "en"
                    "iw", stringResource(R.string.language_iw), "Hebrew", "עברית", "Иврит", "Hébreu" -> "iw"
                    "ru", stringResource(R.string.language_ru), "Russian", "רוסית", "Русский", "Russe" -> "ru"
                    "fr", stringResource(R.string.language_fr), "French", "צרפתית", "Французский", "Français" -> "fr"
                    else -> ""
                }

            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(languageCode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                    if (localeManager.applicationLocales.toLanguageTags() != languageCode) {
                        localeManager.applicationLocales = android.os.LocaleList.forLanguageTags(languageCode)
                    }
                } else {
                    val currentLocale = context.resources.configuration.locales.get(0).language
                    val targetLocale =
                        if (languageCode.isEmpty()) {
                            android.content.res.Resources.getSystem().configuration.locales.get(0).language
                        } else {
                            languageCode
                        }
                    if (currentLocale != targetLocale) {
                        val locale = java.util.Locale(targetLocale)
                        java.util.Locale.setDefault(locale)
                        val resources = context.resources
                        val configuration = resources.configuration
                        configuration.setLocale(locale)
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(configuration, resources.displayMetrics)
                        (context as? android.app.Activity)?.recreate()
                    }
                }
            }

            val currentLocale = context.resources.configuration.locales.get(0)
            val layoutDirection =
                if (android.text.TextUtils.getLayoutDirectionFromLocale(currentLocale) == android.view.View.LAYOUT_DIRECTION_RTL) {
                    androidx.compose.ui.unit.LayoutDirection.Rtl
                } else {
                    androidx.compose.ui.unit.LayoutDirection.Ltr
                }

            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
                AdsBlockTheme(darkTheme = isDarkTheme) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val vpnState by VpnStateManager.state.collectAsState()

                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_home)) },
                                    label = { Text(stringResource(R.string.nav_home)) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        if (currentRoute != "home") {
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    },
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                                    label = { Text(stringResource(R.string.nav_settings)) },
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        if (currentRoute != "settings") {
                                            navController.navigate("settings") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.nav_about)) },
                                    label = { Text(stringResource(R.string.nav_about)) },
                                    selected = currentRoute == "about",
                                    onClick = {
                                        if (currentRoute != "about") {
                                            navController.navigate("about") {
                                                popUpTo("home")
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            composable("home") {
                                HomeScreen(
                                    settingsRepository = settingsRepository,
                                    vpnState = vpnState,
                                    onToggleVpn = { start ->
                                        if (start) {
                                            val vpnIntent = VpnService.prepare(this@MainActivity)
                                            if (vpnIntent != null) {
                                                vpnRequestLauncher.launch(vpnIntent)
                                            } else {
                                                val intent =
                                                    Intent(this@MainActivity, AdsBlockVpnService::class.java).apply {
                                                        action = AdsBlockVpnService.ACTION_START
                                                    }
                                                startService(intent)
                                            }
                                        } else {
                                            val intent =
                                                Intent(this@MainActivity, AdsBlockVpnService::class.java).apply {
                                                    action = AdsBlockVpnService.ACTION_STOP
                                                }
                                            startService(intent)
                                        }
                                    },
                                    onPauseVpn = { duration ->
                                        val intent =
                                            Intent(this@MainActivity, AdsBlockVpnService::class.java).apply {
                                                action = AdsBlockVpnService.ACTION_PAUSE
                                                putExtra(AdsBlockVpnService.EXTRA_PAUSE_DURATION_MINS, duration)
                                            }
                                        startService(intent)
                                    },
                                    onResumeVpn = {
                                        val intent =
                                            Intent(this@MainActivity, AdsBlockVpnService::class.java).apply {
                                                action = AdsBlockVpnService.ACTION_RESUME
                                            }
                                        startService(intent)
                                    },
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    settingsRepository = settingsRepository,
                                    onNavigateToLogs = { navController.navigate("logs") },
                                )
                            }
                            composable("logs") {
                                LogsScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("about") {
                                AboutScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
