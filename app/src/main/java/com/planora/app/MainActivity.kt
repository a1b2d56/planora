package com.planora.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.navigation.PlanoraNavGraph
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.ui.theme.PlanoraTheme
import com.planora.app.core.utils.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load theme from prefs; default to Midnight if not set or error
        var appTheme: AppTheme = AppTheme.MIDNIGHT
        try {
            appTheme = runBlocking { prefsManager.appTheme.first() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeState by prefsManager.appTheme.collectAsStateWithLifecycle(initialValue = appTheme)

            // Request notification permission for Android 13+
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* No action needed */ }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            PlanoraTheme(appTheme = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    PlanoraNavGraph(prefsManager = prefsManager)
                }
            }
        }
    }
}
