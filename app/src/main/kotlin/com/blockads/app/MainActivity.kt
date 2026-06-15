package com.blockads.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.blockads.app.i18n.LocalStrings
import com.blockads.app.i18n.Strings
import com.blockads.app.ui.navigation.AppBottomBar
import com.blockads.app.ui.navigation.AppNavGraph
import com.blockads.app.ui.settings.SettingsViewModel
import com.blockads.app.ui.theme.BlockAdsTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settings by settingsVm.settings.collectAsState()

            val strings =
                remember(settings.languageTag) {
                    val tag =
                        settings.languageTag.ifEmpty {
                            Locale.getDefault().language
                        }
                    Strings.resolve(tag)
                }

            CompositionLocalProvider(LocalStrings provides strings) {
                BlockAdsTheme(themeMode = settings.themeMode) {
                    val navController = rememberNavController()
                    Scaffold(
                        bottomBar = { AppBottomBar(navController) },
                    ) { padding ->
                        AppNavGraph(navController, padding)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
