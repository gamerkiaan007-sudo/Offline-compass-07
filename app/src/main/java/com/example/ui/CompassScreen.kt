package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BearingCourseDialog
import com.example.ui.components.CalibrationDialog
import com.example.ui.components.CompassDial
import com.example.ui.components.CompassHeader
import com.example.ui.components.DeclinationDialog
import com.example.ui.components.TacticalHud
import com.example.ui.theme.OfflineCompassTheme
import kotlinx.coroutines.launch

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showCourseDialog by remember { mutableStateOf(false) }
    var showDeclinationDialog by remember { mutableStateOf(false) }
    var selectedNavTab by remember { mutableIntStateOf(0) }

    // Location Permission launcher for offline GPS declination/coordinates
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updateLocationPermission(granted)
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateLocationPermission(hasFine || hasCoarse)
    }

    // Lifecycle observer to pause sensors in background and resume in foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startSensors()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopSensors()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    OfflineCompassTheme(nightMode = state.nightVisionMode) {
        val colorScheme = MaterialTheme.colorScheme

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                // Sophisticated Dark Bottom Navigation with rounded-t-[32px]
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .testTag("sophisticated_bottom_nav")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Compass Tab
                        NavTabItem(
                            label = "Compass",
                            icon = Icons.Default.Navigation,
                            isSelected = selectedNavTab == 0,
                            onClick = {
                                selectedNavTab = 0
                                coroutineScope.launch { scrollState.animateScrollTo(0) }
                            }
                        )

                        // 2. Level / Incline Tab
                        NavTabItem(
                            label = "Level",
                            icon = Icons.Default.Speed,
                            isSelected = selectedNavTab == 1,
                            onClick = {
                                selectedNavTab = 1
                                showCalibrationDialog = true
                            }
                        )

                        // 3. Info / Declination Tab
                        NavTabItem(
                            label = "Info",
                            icon = Icons.Default.Info,
                            isSelected = selectedNavTab == 2,
                            onClick = {
                                selectedNavTab = 2
                                showDeclinationDialog = true
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Branding, Heading Readout, North Selector & Toggles
                    CompassHeader(
                        state = state,
                        onToggleTrueNorth = { viewModel.toggleTrueNorth() },
                        onToggleNightMode = { viewModel.toggleNightMode() },
                        onToggleHaptic = { viewModel.toggleHaptic() },
                        onLockBearingClick = { viewModel.lockCurrentBearing() },
                        onClearBearingClick = { viewModel.clearLockedBearing() }
                    )

                    // Precision Compass Dial with Bubble Level Reticle & Target Indicator
                    CompassDial(
                        state = state,
                        modifier = Modifier.fillMaxWidth(0.95f),
                        onDialClick = { viewModel.lockCurrentBearing() }
                    )

                    // Sophisticated HUD Telemetry Cards & Controls
                    TacticalHud(
                        state = state,
                        onLockBearing = { viewModel.lockCurrentBearing() },
                        onOpenCourseDialog = { showCourseDialog = true },
                        onOpenDeclinationDialog = { showDeclinationDialog = true },
                        onOpenCalibrationDialog = { showCalibrationDialog = true },
                        onToggleCoordinateFormat = { viewModel.toggleCoordinateFormat() },
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Dialogs
            if (showCalibrationDialog) {
                CalibrationDialog(
                    state = state,
                    onDismiss = {
                        showCalibrationDialog = false
                        selectedNavTab = 0
                    }
                )
            }

            if (showCourseDialog) {
                BearingCourseDialog(
                    state = state,
                    onSetBearing = { bearing -> viewModel.setTargetBearing(bearing) },
                    onClearBearing = { viewModel.clearLockedBearing() },
                    onDismiss = { showCourseDialog = false }
                )
            }

            if (showDeclinationDialog) {
                DeclinationDialog(
                    state = state,
                    onSetManualDeclination = { isManual, offset ->
                        if (isManual != state.manualDeclinationEnabled) {
                            viewModel.toggleManualDeclination()
                        }
                        viewModel.setManualDeclinationValue(offset)
                    },
                    onDismiss = {
                        showDeclinationDialog = false
                        selectedNavTab = 0
                    }
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
        )
    }
}
