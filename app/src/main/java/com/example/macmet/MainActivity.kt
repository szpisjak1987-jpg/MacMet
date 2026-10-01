package com.example.macmet

import android.Manifest
import android.app.DownloadManager
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macmet.data.updater.AppUpdater
import com.example.macmet.data.updater.DownloadReceiver
import com.example.macmet.data.updater.GithubReleaseDto
import com.example.macmet.ui.WeatherScreen
import com.example.macmet.ui.theme.MacóMetTheme
import com.example.macmet.ui.updater.UpdateDialog
import com.example.macmet.ui.weather.WeatherViewModel
import com.example.macmet.ui.weather.WeatherViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as MacMetApplication).container

        setContent {
            MacóMetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val weatherViewModel: WeatherViewModel = viewModel(
                        factory = WeatherViewModelFactory(
                            weatherRepository = appContainer.weatherRepository,
                            locationTracker = appContainer.locationTracker
                        )
                    )

                    val context = LocalContext.current
                    var showUpdateDialog by remember { mutableStateOf(false) }
                    var availableRelease by remember { mutableStateOf<GithubReleaseDto?>(null) }
                    var downloadId by remember { mutableStateOf<Long?>(null) }
                    val appUpdater = remember { AppUpdater(context) }

                    // Register DownloadReceiver when download starts
                    DisposableEffect(downloadId) {
                        if (downloadId != null) {
                            val receiver = DownloadReceiver(downloadId!!)
                            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.registerReceiver(receiver, filter, ContextCompat.RECEIVER_EXPORTED)
                            } else {
                                context.registerReceiver(receiver, filter)
                            }
                            onDispose {
                                try {
                                    context.unregisterReceiver(receiver)
                                } catch (_: Exception) {}
                            }
                        } else {
                            onDispose {}
                        }
                    }

                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val isGranted = permissions.values.any { it }
                        weatherViewModel.onPermissionResult(isGranted)
                    }

                    LaunchedEffect(Unit) {
                        // Check for GitHub updates silently
                        try {
                            val currentVersion = BuildConfig.VERSION_NAME
                            Log.d("AppUpdater", "MainActivity checking update for version: $currentVersion")
                            val (isUpdateAvailable, release) = appUpdater.checkForUpdate(currentVersion)
                            if (isUpdateAvailable && release != null && release.assets?.isNotEmpty() == true) {
                                availableRelease = release
                                showUpdateDialog = true
                            }
                        } catch (e: Exception) {
                            Log.e("AppUpdater", "Error checking for updates in MainActivity", e)
                        }

                        val fineGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        val coarseGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        weatherViewModel.updatePermissionStatus(fineGranted || coarseGranted)
                    }

                        WeatherScreen(
                            viewModel = weatherViewModel,
                            onRequestPermission = {
                                val permissions = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                locationPermissionLauncher.launch(permissions.toTypedArray())
                            }
                        )

                    if (showUpdateDialog && availableRelease != null) {
                        val release = availableRelease!!
                        UpdateDialog(
                            newVersion = release.tagName,
                            releaseNotes = release.body,
                            onDismiss = { showUpdateDialog = false },
                            onDownload = {
                                val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk") }
                                if (apkAsset != null) {
                                    downloadId = appUpdater.downloadUpdate(apkAsset.browserDownloadUrl, release.tagName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
