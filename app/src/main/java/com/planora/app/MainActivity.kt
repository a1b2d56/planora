package com.planora.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.core.security.BiometricGuard
import com.planora.app.navigation.PlanoraNavGraph
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.ui.theme.PlanoraTheme
import com.planora.app.core.utils.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var prefsManager: PrefsManager
    private lateinit var biometricGuard: BiometricGuard

    override fun onCreate(savedInstanceState: Bundle?) {
        var appTheme: AppTheme = AppTheme.DARK
        try {
            appTheme = runBlocking { prefsManager.appTheme.first() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        biometricGuard = BiometricGuard(this)

        setContent {
            val themeState by prefsManager.appTheme.collectAsStateWithLifecycle(initialValue = appTheme)
            var isAuthenticated by remember { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                val biometricEnabled = prefsManager.biometricEnabled.first()
                if (biometricEnabled && biometricGuard.isBiometricAvailable() && !isAuthenticated) {
                    biometricGuard.authenticate(
                        onSuccess = { isAuthenticated = true },
                        onError = { isAuthenticated = true }
                    )
                } else {
                    isAuthenticated = true
                }
            }

            PlanoraTheme(appTheme = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    if (isAuthenticated) {
                        PlanoraNavGraph(prefsManager = prefsManager)
                    } else {
                         // Show nothing, just background, waiting for biometric
                         Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
