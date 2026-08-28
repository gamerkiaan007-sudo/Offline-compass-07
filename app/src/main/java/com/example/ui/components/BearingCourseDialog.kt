package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompassState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BearingCourseDialog(
    state: CompassState,
    onSetBearing: (Float) -> Unit,
    onClearBearing: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedAngle by remember {
        mutableFloatStateOf(state.lockedBearing ?: state.effectiveHeading)
    }

    val quickPresets = listOf(
        Pair("North (0°)", 0f),
        Pair("NE (45°)", 45f),
        Pair("East (90°)", 90f),
        Pair("SE (135°)", 135f),
        Pair("South (180°)", 180f),
        Pair("SW (225°)", 225f),
        Pair("West (270°)", 270f),
        Pair("NW (315°)", 315f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("bearing_course_dialog"),
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceVariant,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Target Course Bearing",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select a target course heading to track directional deviation and stay on course.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current Selected Angle Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format(Locale.US, "%03d°", selectedAngle.roundToInt() % 360),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp
                        ),
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CompassState.headingToCardinal(selectedAngle),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Slider(
                    value = selectedAngle,
                    onValueChange = { selectedAngle = it },
                    valueRange = 0f..359f,
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.primary,
                        activeTrackColor = colorScheme.primary,
                        inactiveTrackColor = colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bearing_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QUICK PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(140.dp)
                ) {
                    items(quickPresets) { (label, deg) ->
                        FilledTonalButton(
                            onClick = { selectedAngle = deg },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetBearing(selectedAngle)
                    onDismiss()
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("apply_bearing_button")
            ) {
                Text("Set Target", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            if (state.lockedBearing != null) {
                TextButton(
                    onClick = {
                        onClearBearing()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("clear_dialog_bearing_button")
                ) {
                    Text("Clear Lock", color = colorScheme.onSurfaceVariant)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
