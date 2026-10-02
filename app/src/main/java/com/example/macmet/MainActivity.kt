package com.example.macmet

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.macmet.data.model.api.GithubReleaseDto
import com.example.macmet.data.updater.AppUpdater
import com.example.macmet.data.updater.DownloadReceiver
import com.example.macmet.ui.updater.UpdateDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macmet.ui.screens.ClimateResearchScreen
import com.example.macmet.ui.screens.ForecastScreen
import com.example.macmet.ui.screens.RadarScreen
import com.example.macmet.ui.screens.SearchLocationsDialog
import com.example.macmet.ui.theme.MyApplicationTheme
import com.example.macmet.viewmodel.WeatherUiState
import com.example.macmet.viewmodel.WeatherViewModel

enum class AppTab(val label: String) {
    FORECAST("Előrejelzés"),
    RADAR("Radar"),
    RESEARCH("Kutatás")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainWeatherApp()
            }
        }
    }
}

@Composable
fun MainWeatherApp(
    viewModel: WeatherViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val radarState by viewModel.radarState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.FORECAST) }
    var isSearchDialogOpen by remember { mutableStateOf(false) }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.loadWeatherForCurrentLocation()
        }
    }

    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<GithubReleaseDto?>(null) }
    var downloadId by remember { mutableStateOf<Long?>(null) }
    val appUpdater = remember { AppUpdater(context) }

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
                try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(Unit) {
        // Request permissions
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(permissions.toTypedArray())

        // Check for GitHub updates silently
        try {
            val currentVersion = BuildConfig.VERSION_NAME
            val (isUpdateAvailable, release) = appUpdater.checkForUpdate(currentVersion)
            if (isUpdateAvailable && release != null && release.assets?.isNotEmpty() == true) {
                availableRelease = release
                showUpdateDialog = true
            }
        } catch (e: Exception) {
            Log.e("AppUpdater", "Error checking for update", e)
        }
    }

    // Handle back press if in secondary tab or dialog
    BackHandler(enabled = currentTab != AppTab.FORECAST || isSearchDialogOpen) {
        if (isSearchDialogOpen) {
            isSearchDialogOpen = false
        } else {
            currentTab = AppTab.FORECAST
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                // Tab 1: Forecast
                NavigationBarItem(
                    selected = currentTab == AppTab.FORECAST,
                    onClick = { currentTab = AppTab.FORECAST },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.FORECAST) Icons.Filled.WbSunny else Icons.Outlined.WbSunny,
                            contentDescription = "Időjárás előrejelzés"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.FORECAST.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_forecast")
                )

                // Tab 2: RainViewer Radar
                NavigationBarItem(
                    selected = currentTab == AppTab.RADAR,
                    onClick = { currentTab = AppTab.RADAR },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.RADAR) Icons.Filled.Radar else Icons.Outlined.Radar,
                            contentDescription = "Csapadékradar"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.RADAR.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_radar")
                )

                // Tab 3: Climate Research & Sources
                NavigationBarItem(
                    selected = currentTab == AppTab.RESEARCH,
                    onClick = { currentTab = AppTab.RESEARCH },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.RESEARCH) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Kutatási háttér és nyílt adatok"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.RESEARCH.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_research")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            when (currentTab) {
                AppTab.FORECAST -> {
                    ForecastScreen(
                        uiState = uiState,
                        onRefresh = { viewModel.refresh() },
                        onOpenSearch = { isSearchDialogOpen = true },
                        onToggleFavorite = { viewModel.toggleFavorite() }
                    )
                }

                AppTab.RADAR -> {
                    val locationName = when (val s = uiState) {
                        is WeatherUiState.Success -> s.data.locationName
                        is WeatherUiState.Error -> s.fallbackData?.locationName ?: "Közép-Európa"
                        WeatherUiState.Loading -> "Közép-Európa"
                    }
                    val lat = when (val s = uiState) {
                        is WeatherUiState.Success -> s.data.latitude
                        is WeatherUiState.Error -> s.fallbackData?.latitude ?: 47.4979
                        WeatherUiState.Loading -> 47.4979
                    }
                    val lon = when (val s = uiState) {
                        is WeatherUiState.Success -> s.data.longitude
                        is WeatherUiState.Error -> s.fallbackData?.longitude ?: 19.0402
                        WeatherUiState.Loading -> 19.0402
                    }

                    RadarScreen(
                        radarState = radarState,
                        currentLocationName = locationName,
                        currentLat = lat,
                        currentLon = lon,
                        onTogglePlay = { viewModel.toggleRadarPlayback() },
                        onSelectFrame = { viewModel.selectRadarFrame(it) },
                        onRefresh = { viewModel.loadRadarData() }
                    )
                }

                AppTab.RESEARCH -> {
                    ClimateResearchScreen()
                }
            }

            // Search Dialog
            if (isSearchDialogOpen) {
                SearchLocationsDialog(
                    searchState = searchState,
                    savedLocations = savedLocations,
                    onQueryChange = { viewModel.searchCity(it) },
                    onSelectLocation = { lat, lon, name, country ->
                        viewModel.loadWeatherForCoordinates(lat, lon, name, country)
                    },
                    onSelectCurrentLocation = {
                        viewModel.loadWeatherForCurrentLocation()
                    },
                    onDeleteSavedLocation = { id ->
                        viewModel.deleteSavedLocation(id)
                    },
                    onDismiss = {
                        isSearchDialogOpen = false
                        viewModel.clearSearch()
                    }
                )
            }

            // GitHub Update Dialog
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
