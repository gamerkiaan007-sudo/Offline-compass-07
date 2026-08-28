package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassHeader(
    state: CompassState,
    onToggleTrueNorth: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleHaptic: () -> Unit,
    onLockBearingClick: () -> Unit,
    onClearBearingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val headingInt = state.effectiveHeading.roundToInt() % 360
    val cardinal = state.cardinalDirection

    // Infinite pulse for precision status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar: Sophisticated header with rounded circle action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action / Compass Branding Icon
            Surface(
                shape = CircleShape,
                color = colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("app_branding_row")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Central Clean Title
            Text(
                text = "Compass",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = colorScheme.onSurface
            )

            // Right Quick Controls (Haptic & Night Vision)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Haptic feedback pill button
                Surface(
                    shape = CircleShape,
                    color = if (state.hapticFeedback) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onToggleHaptic() }
                        .testTag("haptic_toggle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.hapticFeedback) Icons.Filled.Vibration else Icons.Outlined.Vibration,
                            contentDescription = "Toggle Haptic Feedback",
                            tint = if (state.hapticFeedback) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Night mode toggle pill button
                Surface(
                    shape = CircleShape,
                    color = if (state.nightVisionMode) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onToggleNightMode() }
                        .testTag("night_mode_toggle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.nightVisionMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Night Vision Mode",
                            tint = if (state.nightVisionMode) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Elegant Digital Readout matching Sophisticated Dark typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag("heading_display_row")
        ) {
            Text(
                text = String.format(Locale.US, "%03d°", headingInt),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp
                ),
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Lavender Cardinal Direction Subtitle
            Text(
                text = cardinal.uppercase(Locale.US),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    fontSize = 14.sp
                ),
                color = colorScheme.primary,
                modifier = Modifier.testTag("cardinal_tag")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // North Mode Pill Chip & Status Badge Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // True North vs Magnetic North Selector Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggleTrueNorth() }
                    .testTag("north_mode_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.useTrueNorth) {
                            val decStr = String.format(
                                Locale.US,
                                "%s%.1f°",
                                if (state.effectiveDeclination >= 0) "+" else "",
                                state.effectiveDeclination
                            )
                            "True North ($decStr)"
                        } else {
                            "Magnetic North"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            fontSize = 11.5.sp
                        ),
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // High Precision • Offline Mode status badge with animated glowing pulse
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                (if (state.nightVisionMode) colorScheme.primary else SophisticatedPrecisionGreen)
                                    .copy(alpha = pulseAlpha)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Offline Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            fontSize = 11.5.sp
                        ),
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Locked Bearing Banner (if active)
        AnimatedVisibility(
            visible = state.lockedBearing != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            state.lockedBearing?.let { target ->
                val diff = state.bearingDeviation ?: 0f
                val diffInt = abs(diff.roundToInt())
                val directionText = when {
                    diffInt <= 1 -> "ON COURSE 🎯"
                    diff > 0 -> "TURN RIGHT $diffInt° ▶"
                    else -> "◀ TURN LEFT $diffInt°"
                }
                val deviationColor = when {
                    diffInt <= 2 -> SophisticatedPrecisionGreen
                    diffInt <= 15 -> SophisticatedWarning
                    else -> SophisticatedError
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("bearing_lock_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "TARGET: %03d°", target.roundToInt()),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = directionText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (state.nightVisionMode) colorScheme.primary else deviationColor
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = colorScheme.surface,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onClearBearingClick() }
                                .testTag("clear_bearing_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Target Bearing",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
