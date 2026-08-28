package com.example.ui.components

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CompassState
import com.example.ui.theme.SophisticatedPrecisionGreen
import com.example.ui.theme.SophisticatedWarning
import com.example.ui.theme.SophisticatedError
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompassDial(
    state: CompassState,
    modifier: Modifier = Modifier,
    onDialClick: () -> Unit = {}
) {
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme

    val primaryColor = colorScheme.primary
    val onPrimaryColor = colorScheme.onPrimary
    val surfaceColor = colorScheme.surface
    val surfaceVariantColor = colorScheme.surfaceVariant
    val outlineColor = colorScheme.outline
    val textPrimary = colorScheme.onSurface
    val textSecondary = colorScheme.onSurfaceVariant
    val levelGreen = if (state.nightVisionMode) colorScheme.primary else SophisticatedPrecisionGreen
    val bearingColor = if (state.nightVisionMode) colorScheme.primary else colorScheme.primary

    // Smooth transition for bubble level
    val animatedPitch by animateFloatAsState(
        targetValue = state.pitch.coerceIn(-45f, 45f),
        animationSpec = tween(durationMillis = 60),
        label = "pitchAnim"
    )
    val animatedRoll by animateFloatAsState(
        targetValue = state.roll.coerceIn(-45f, 45f),
        animationSpec = tween(durationMillis = 60),
        label = "rollAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp)
            .testTag("compass_dial_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDialClick
                )
                .testTag("compass_dial_canvas")
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 6.dp.toPx()
            val dialRotation = -state.effectiveHeading

            // 1. Draw Sophisticated Housing (outer border + inner dashed track)
            drawSophisticatedHousing(center, radius, surfaceColor, surfaceVariantColor, outlineColor)

            // 2. Draw rotating compass rose & graduation dial
            rotate(dialRotation, pivot = center) {
                drawRotatingDial(
                    center = center,
                    radius = radius,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor,
                    surfaceVariantColor = surfaceVariantColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    outlineColor = outlineColor,
                    lockedBearing = state.lockedBearing,
                    bearingColor = bearingColor
                )
            }

            // 3. Draw Target Bearing Deviation Arc & Needle (if bearing locked)
            if (state.lockedBearing != null) {
                drawBearingDeviationArc(
                    center = center,
                    radius = radius,
                    currentHeading = state.effectiveHeading,
                    targetBearing = state.lockedBearing,
                    bearingColor = bearingColor
                )
            }

            // 4. Center 2D Bubble Level & Inclinometer Crosshairs
            drawBubbleLevel(
                center = center,
                radius = radius * 0.38f,
                pitch = animatedPitch,
                roll = animatedRoll,
                isLevel = state.isLevel,
                levelColor = levelGreen,
                primaryColor = primaryColor,
                outlineColor = outlineColor,
                surfaceColor = surfaceColor
            )

            // 5. Static Top Lubber Line / Heading Index
            drawTopLubberLine(
                center = center,
                radius = radius,
                indexColor = primaryColor
            )
        }
    }
}

private fun DrawScope.drawSophisticatedHousing(
    center: Offset,
    radius: Float,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    outlineColor: Color
) {
    // Outer subtle gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                surfaceVariantColor.copy(alpha = 0.5f),
                surfaceColor,
                Color.Black.copy(alpha = 0.6f)
            ),
            center = center,
            radius = radius + 6.dp.toPx()
        ),
        radius = radius + 4.dp.toPx(),
        center = center
    )

    // Outer solid border ring (border-2 border-[#49454F])
    drawCircle(
        color = outlineColor.copy(alpha = 0.7f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // Inner dashed track ring (border-1 border-[#49454F] border-dashed)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f)
    drawCircle(
        color = outlineColor.copy(alpha = 0.5f),
        radius = radius - 14.dp.toPx(),
        center = center,
        style = Stroke(
            width = 1.2.dp.toPx(),
            pathEffect = dashEffect
        )
    )
}

private fun DrawScope.drawRotatingDial(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    onPrimaryColor: Color,
    surfaceVariantColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    outlineColor: Color,
    lockedBearing: Float?,
    bearingColor: Color
) {
    val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val tickRadiusOuter = radius - 2.dp.toPx()
    val majorTickLen = 12.dp.toPx()
    val medTickLen = 8.dp.toPx()
    val minorTickLen = 4.5.dp.toPx()

    // Draw degree tick marks around 360°
    for (deg in 0 until 360 step 2) {
        val angleRad = (deg - 90) * (PI / 180.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val isMajor = deg % 30 == 0
        val isMedium = deg % 10 == 0 && !isMajor
        val tickLen = when {
            isMajor -> majorTickLen
            isMedium -> medTickLen
            else -> minorTickLen
        }

        val tickColor = when {
            deg == 0 -> primaryColor
            isMajor -> primaryColor.copy(alpha = 0.8f)
            isMedium -> textSecondary.copy(alpha = 0.6f)
            else -> outlineColor.copy(alpha = 0.4f)
        }

        val strokeW = when {
            deg == 0 -> 2.5.dp.toPx()
            isMajor -> 1.8.dp.toPx()
            isMedium -> 1.2.dp.toPx()
            else -> 0.9.dp.toPx()
        }

        val p1 = Offset(center.x + (tickRadiusOuter) * cosA, center.y + (tickRadiusOuter) * sinA)
        val p2 = Offset(center.x + (tickRadiusOuter - tickLen) * cosA, center.y + (tickRadiusOuter - tickLen) * sinA)

        drawLine(
            color = tickColor,
            start = p1,
            end = p2,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Draw degree numerals every 30° (except at 0, 90, 180, 270 where cardinal badges sit)
        if (isMajor && deg % 30 == 0 && deg != 0 && deg != 90 && deg != 180 && deg != 270) {
            val numRadius = tickRadiusOuter - majorTickLen - 12.dp.toPx()
            val textX = center.x + numRadius * cosA
            val textY = center.y + numRadius * sinA

            val numText = String.format(Locale.US, "%03d", deg)
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(deg.toFloat(), textX, textY)
                textPaint.color = textSecondary.toArgb()
                textPaint.textSize = 9.dp.toPx()
                textPaint.isFakeBoldText = false
                val bounds = Rect()
                textPaint.getTextBounds(numText, 0, numText.length, bounds)
                drawText(numText, textX, textY + bounds.height() / 2f, textPaint)
                restore()
            }
        }
    }

    // Draw Sophisticated Dual-tone Pointer Needle (North: Lavender + Glow, South: Slate outline)
    val needleLen = radius * 0.72f
    val needleHalfW = 8.dp.toPx()

    // North Needle with subtle lavender glow
    val northNeedlePath = Path().apply {
        moveTo(center.x, center.y - needleLen)
        lineTo(center.x - needleHalfW, center.y - 10.dp.toPx())
        lineTo(center.x + needleHalfW, center.y - 10.dp.toPx())
        close()
    }
    // Glow under North needle
    drawPath(
        path = northNeedlePath,
        brush = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = 0.5f), primaryColor.copy(alpha = 0.0f)),
            center = Offset(center.x, center.y - needleLen / 2f),
            radius = needleLen / 1.5f
        )
    )
    drawPath(northNeedlePath, color = primaryColor, style = Fill)

    // South Needle (Slate)
    val southNeedlePath = Path().apply {
        moveTo(center.x, center.y + needleLen)
        lineTo(center.x - needleHalfW, center.y + 10.dp.toPx())
        lineTo(center.x + needleHalfW, center.y + 10.dp.toPx())
        close()
    }
    drawPath(southNeedlePath, color = outlineColor, style = Fill)

    // Draw Cardinal & Ordinal Points with Elegant Pill Badges
    val cardinalBadges = listOf(
        Pair(0, "N"),
        Pair(90, "E"),
        Pair(180, "S"),
        Pair(270, "W")
    )

    cardinalBadges.forEach { (deg, label) ->
        val isNorth = deg == 0
        val angleRad = (deg - 90) * (PI / 180.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val badgeRadius = tickRadiusOuter - majorTickLen - 14.dp.toPx()
        val badgeX = center.x + badgeRadius * cosA
        val badgeY = center.y + badgeRadius * sinA

        val pillWidth = 24.dp.toPx()
        val pillHeight = 18.dp.toPx()

        drawContext.canvas.nativeCanvas.apply {
            save()
            rotate(deg.toFloat(), badgeX, badgeY)

            // Draw pill background
            val pillBgColor = if (isNorth) primaryColor else surfaceVariantColor
            val pillRect = RoundRect(
                left = badgeX - pillWidth / 2f,
                top = badgeY - pillHeight / 2f,
                right = badgeX + pillWidth / 2f,
                bottom = badgeY + pillHeight / 2f,
                cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
            )

            // Outer border for secondary cardinal pills
            if (!isNorth) {
                drawRoundRect(
                    color = outlineColor.copy(alpha = 0.6f),
                    topLeft = Offset(pillRect.left, pillRect.top),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            drawRoundRect(
                color = pillBgColor,
                topLeft = Offset(pillRect.left, pillRect.top),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
                style = Fill
            )

            // Text
            textPaint.color = (if (isNorth) onPrimaryColor else textPrimary).toArgb()
            textPaint.textSize = 11.dp.toPx()
            textPaint.isFakeBoldText = true
            val bounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, bounds)
            drawText(label, badgeX, badgeY + bounds.height() / 2f, textPaint)

            restore()
        }
    }

    // Ordinal Points (NE, SE, SW, NW)
    val ordinals = listOf(
        Pair(45, "NE"),
        Pair(135, "SE"),
        Pair(225, "SW"),
        Pair(315, "NW")
    )
    ordinals.forEach { (deg, label) ->
        val angleRad = (deg - 90) * (PI / 180.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val ordRadius = tickRadiusOuter - medTickLen - 12.dp.toPx()
        val ordX = center.x + ordRadius * cosA
        val ordY = center.y + ordRadius * sinA

        drawContext.canvas.nativeCanvas.apply {
            save()
            rotate(deg.toFloat(), ordX, ordY)
            textPaint.color = textSecondary.toArgb()
            textPaint.textSize = 10.dp.toPx()
            textPaint.isFakeBoldText = false
            val bounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, bounds)
            drawText(label, ordX, ordY + bounds.height() / 2f, textPaint)
            restore()
        }
    }

    // Locked bearing indicator pip on dial
    if (lockedBearing != null) {
        val bAngleRad = (lockedBearing - 90) * (PI / 180.0)
        val bCos = cos(bAngleRad).toFloat()
        val bSin = sin(bAngleRad).toFloat()
        val bPipRadius = radius - 4.dp.toPx()
        val pipCenter = Offset(center.x + bPipRadius * bCos, center.y + bPipRadius * bSin)

        drawCircle(
            color = bearingColor,
            radius = 5.dp.toPx(),
            center = pipCenter
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = pipCenter
        )
    }
}

private fun DrawScope.drawBearingDeviationArc(
    center: Offset,
    radius: Float,
    currentHeading: Float,
    targetBearing: Float,
    bearingColor: Color
) {
    var diff = (targetBearing - currentHeading) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f

    val arcRadius = radius - 24.dp.toPx()
    val arcSize = Size(arcRadius * 2, arcRadius * 2)
    val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

    val startAngle = -90f
    val sweepAngle = diff

    drawArc(
        color = bearingColor.copy(alpha = 0.5f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )

    val destAngleRad = (startAngle + sweepAngle) * (PI / 180.0)
    val targetPipX = center.x + arcRadius * cos(destAngleRad).toFloat()
    val targetPipY = center.y + arcRadius * sin(destAngleRad).toFloat()

    drawCircle(
        color = bearingColor,
        radius = 4.5.dp.toPx(),
        center = Offset(targetPipX, targetPipY)
    )
}

private fun DrawScope.drawBubbleLevel(
    center: Offset,
    radius: Float,
    pitch: Float,
    roll: Float,
    isLevel: Boolean,
    levelColor: Color,
    primaryColor: Color,
    outlineColor: Color,
    surfaceColor: Color
) {
    // Level boundary circle in Charcoal #1C1B1F
    drawCircle(
        color = surfaceColor,
        radius = radius,
        center = center
    )
    drawCircle(
        color = outlineColor.copy(alpha = 0.6f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Inner reference rings
    drawCircle(
        color = outlineColor.copy(alpha = 0.35f),
        radius = radius * 0.6f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = if (isLevel) levelColor else primaryColor.copy(alpha = 0.4f),
        radius = radius * 0.25f,
        center = center,
        style = Stroke(width = if (isLevel) 2.dp.toPx() else 1.2.dp.toPx())
    )

    // Crosshair reticle lines
    val reticleLen = radius * 0.85f
    val reticleColor = if (isLevel) levelColor.copy(alpha = 0.6f) else outlineColor.copy(alpha = 0.4f)
    drawLine(
        color = reticleColor,
        start = Offset(center.x - reticleLen, center.y),
        end = Offset(center.x + reticleLen, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = reticleColor,
        start = Offset(center.x, center.y - reticleLen),
        end = Offset(center.x, center.y + reticleLen),
        strokeWidth = 1.dp.toPx()
    )

    // Bubble offset calculation
    val maxTilt = 30f
    val offsetX = (roll / maxTilt).coerceIn(-1f, 1f) * (radius * 0.75f)
    val offsetY = (-pitch / maxTilt).coerceIn(-1f, 1f) * (radius * 0.75f)
    val bubbleCenter = Offset(center.x + offsetX, center.y + offsetY)
    val bubbleRadius = radius * 0.2f

    // Bubble visual
    val bubbleColor = if (isLevel) levelColor else primaryColor
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bubbleColor.copy(alpha = 0.85f),
                bubbleColor.copy(alpha = 0.35f)
            ),
            center = bubbleCenter,
            radius = bubbleRadius
        ),
        radius = bubbleRadius,
        center = bubbleCenter
    )
    drawCircle(
        color = bubbleColor,
        radius = bubbleRadius,
        center = bubbleCenter,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawTopLubberLine(
    center: Offset,
    radius: Float,
    indexColor: Color
) {
    val topY = center.y - radius
    val triHeight = 11.dp.toPx()
    val triHalfW = 6.dp.toPx()

    val path = Path().apply {
        moveTo(center.x, topY + triHeight)
        lineTo(center.x - triHalfW, topY - 2.dp.toPx())
        lineTo(center.x + triHalfW, topY - 2.dp.toPx())
        close()
    }

    drawPath(path, color = indexColor, style = Fill)
}
