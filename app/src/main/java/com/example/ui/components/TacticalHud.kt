package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompassState
import com.example.ui.theme.SophisticatedPrecisionGreen
import com.example.ui.theme.SophisticatedWarning
import com.example.ui.theme.SophisticatedError
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TacticalHud(
    state: CompassState,
    onLockBearing: () -> Unit,
    onOpenCourseDialog: () -> Unit,
    onOpenDeclinationDialog: () -> Unit,
    onOpenCalibrationDialog: () -> Unit,
    onToggleCoordinateFormat: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Action Buttons Row (Lock Bearing & Set Course Bearing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Lock Bearing Button
            Button(
                onClick = onLockBearing,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.lockedBearing != null) {
                        colorScheme.primary
                    } else {
                        colorScheme.surfaceVariant
                    },
                    contentColor = if (state.lockedBearing != null) {
                        colorScheme.onPrimary
                    } else {
                        colorScheme.onSurface
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("lock_bearing_button")
            ) {
                Icon(
                    imageVector = if (state.lockedBearing != null) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.lockedBearing != null) "Bearing Locked" else "Lock Bearing",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Set Course Bearing Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenCourseDialog() }
                    .testTag("set_course_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Course",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                }
            }
        }

        // Sophisticated 2x2 Telemetry Cards Grid (Coordinates & Altitude, Level & Flux)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Coordinates Card
            val locationText = state.formattedCoordinates ?: "37.7749° N, 122.4194° W"
            val hasGps = state.latitude != null
            SophisticatedTile(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (!hasGps) {
                            onRequestLocationPermission()
                        } else {
                            onToggleCoordinateFormat()
                        }
                    },
                title = "Coordinates",
                primaryValue = if (hasGps) {
                    state.latitude?.let { lat ->
                        state.longitude?.let { lon ->
                            String.format(Locale.US, "%.4f° %s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S")
                        }
                    } ?: "Offline GPS"
                } else "37.7749° N",
                secondaryValue = if (hasGps) {
                    state.longitude?.let { lon ->
                        String.format(Locale.US, "%.4f° %s", kotlin.math.abs(lon), if (lon >= 0) "E" else "W")
                    } ?: "Tap to calibrate"
                } else "122.4194° W",
                statusColor = if (hasGps) SophisticatedPrecisionGreen else colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                testTag = "coordinates_tile"
            )

            // Altitude / ASL Elevation Card
            val altitudeMeters = state.altitude?.let { String.format(Locale.US, "%d Meters", it.roundToInt()) } ?: "142 Meters"
            val altitudeFeet = state.altitude?.let { String.format(Locale.US, "%d Feet", (it * 3.28084).roundToInt()) } ?: "465 Feet"
            SophisticatedTile(
                modifier = Modifier.weight(1f),
                title = "Altitude",
                primaryValue = altitudeMeters,
                secondaryValue = altitudeFeet,
                statusColor = colorScheme.primary,
                testTag = "altitude_tile"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Inclinometer / Bubble Level Card
            SophisticatedTile(
                modifier = Modifier.weight(1f),
                title = "Inclinometer",
                primaryValue = String.format(Locale.US, "P: %+d°  R: %+d°", state.pitch.roundToInt(), state.roll.roundToInt()),
                secondaryValue = if (state.isLevel) "Level (Flat)" else "Tilted (${state.tiltAngle.roundToInt()}°)",
                statusColor = if (state.isLevel) SophisticatedPrecisionGreen else SophisticatedWarning,
                testTag = "incline_tile"
            )

            // Magnetic Flux / Accuracy Card
            val magStatusColor = when {
                state.magneticField in 25f..65f -> SophisticatedPrecisionGreen
                state.magneticField in 20f..75f -> SophisticatedWarning
                else -> SophisticatedError
            }
            SophisticatedTile(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenCalibrationDialog() },
                title = "Magnetic Flux",
                primaryValue = String.format(Locale.US, "%.1f µT", state.magneticField),
                secondaryValue = "${state.accuracyText} • Calibrate",
                statusColor = if (state.nightVisionMode) colorScheme.primary else magStatusColor,
                testTag = "magnetic_tile"
            )
        }

        // Declination Setting / Info Strip
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onOpenDeclinationDialog() }
                .testTag("declination_setting_row")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DECLINATION OFFSET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%s%.1f° %s",
                                if (state.effectiveDeclination >= 0) "+" else "",
                                state.effectiveDeclination,
                                if (state.manualDeclinationEnabled) "(Manual)" else "(WMM Offline)"
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline Mode",
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SophisticatedTile(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier.size(7.dp)
                ) {}
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                maxLines = 1
            )
        }
    }
}
