package com.example.model

import android.hardware.SensorManager
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CompassState(
    val azimuth: Float = 0f,
    val rawAzimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val magneticField: Float = 45f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val declination: Float = 0f,
    val useTrueNorth: Boolean = false,
    val lockedBearing: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val hasLocationPermission: Boolean = false,
    val hapticFeedback: Boolean = true,
    val nightVisionMode: Boolean = false,
    val coordinateFormatDms: Boolean = true,
    val manualDeclinationEnabled: Boolean = false,
    val manualDeclinationValue: Float = 0f,
    val isSensorAvailable: Boolean = true
) {
    val effectiveDeclination: Float
        get() = if (manualDeclinationEnabled) manualDeclinationValue else declination

    val effectiveHeading: Float
        get() {
            val base = if (useTrueNorth) (azimuth + effectiveDeclination) else azimuth
            return (base % 360f + 360f) % 360f
        }

    val tiltAngle: Float
        get() = sqrt((pitch * pitch + roll * roll).toDouble()).toFloat()

    val isLevel: Boolean
        get() = tiltAngle < 2.5f

    val cardinalDirection: String
        get() = headingToCardinal(effectiveHeading)

    val bearingDeviation: Float?
        get() {
            val target = lockedBearing ?: return null
            var diff = (effectiveHeading - target) % 360f
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            return diff
        }

    val formattedCoordinates: String?
        get() {
            val lat = latitude ?: return null
            val lon = longitude ?: return null
            return if (coordinateFormatDms) {
                "${toDms(lat, true)}, ${toDms(lon, false)}"
            } else {
                String.format(Locale.US, "%.5f° %s, %.5f° %s",
                    abs(lat), if (lat >= 0) "N" else "S",
                    abs(lon), if (lon >= 0) "E" else "W"
                )
            }
        }

    val magneticFieldStatus: String
        get() = when {
            magneticField < 25f -> "Weak Field"
            magneticField in 25f..65f -> "Normal (Earth)"
            else -> "Strong Interference"
        }

    val accuracyText: String
        get() = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High Precision"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium Precision"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low Precision"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "Uncalibrated"
            else -> "Calibrated"
        }

    companion object {
        fun headingToCardinal(heading: Float): String {
            val normalized = (heading % 360f + 360f) % 360f
            val directions = arrayOf(
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
            )
            val index = ((normalized + 11.25f) / 22.5f).toInt() % 16
            return directions[index]
        }

        fun toDms(value: Double, isLatitude: Boolean): String {
            val absolute = abs(value)
            val degrees = absolute.toInt()
            val minutesDouble = (absolute - degrees) * 60.0
            val minutes = minutesDouble.toInt()
            val seconds = ((minutesDouble - minutes) * 60.0).roundToInt()
            val direction = if (isLatitude) {
                if (value >= 0) "N" else "S"
            } else {
                if (value >= 0) "E" else "W"
            }
            return String.format(Locale.US, "%d°%02d'%02d\" %s", degrees, minutes, seconds, direction)
        }
    }
}
