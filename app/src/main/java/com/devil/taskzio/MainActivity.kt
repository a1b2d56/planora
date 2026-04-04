package com.devil.taskzio

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
import com.devil.taskzio.navigation.TaskzioNavGraph
import com.devil.taskzio.theme.AppTheme
import com.devil.taskzio.theme.TaskzioTheme
import com.devil.taskzio.utils.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Suppress("unused")
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

            TaskzioTheme(appTheme = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    TaskzioNavGraph(prefsManager = prefsManager)
                }
            }
        }
    }
}
