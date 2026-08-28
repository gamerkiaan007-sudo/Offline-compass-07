package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CompassState
import com.example.sensor.CompassSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager = CompassSensorManager(application.applicationContext)

    private val _state = MutableStateFlow(CompassState())
    val state: StateFlow<CompassState> = _state.asStateFlow()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var lastHapticHeading = -1
    private var lastHapticTime = 0L

    init {
        viewModelScope.launch {
            sensorManager.sensorDataFlow.collectLatest { sensorData ->
                _state.update { current ->
                    current.copy(
                        azimuth = sensorData.azimuth,
                        pitch = sensorData.pitch,
                        roll = sensorData.roll,
                        magneticField = sensorData.magneticField,
                        accuracy = sensorData.accuracy,
                        declination = sensorData.declination,
                        latitude = sensorData.latitude,
                        longitude = sensorData.longitude,
                        altitude = sensorData.altitude,
                        isSensorAvailable = sensorData.isSensorAvailable
                    )
                }
                checkHapticFeedback(_state.value)
            }
        }
    }

    fun startSensors() {
        sensorManager.startListening()
    }

    fun stopSensors() {
        sensorManager.stopListening()
    }

    fun toggleTrueNorth() {
        _state.update { it.copy(useTrueNorth = !it.useTrueNorth) }
        triggerSubtleTick()
    }

    fun toggleNightMode() {
        _state.update { it.copy(nightVisionMode = !it.nightVisionMode) }
        triggerSubtleTick()
    }

    fun toggleHaptic() {
        _state.update { it.copy(hapticFeedback = !it.hapticFeedback) }
        triggerSubtleTick()
    }

    fun toggleCoordinateFormat() {
        _state.update { it.copy(coordinateFormatDms = !it.coordinateFormatDms) }
    }

    fun lockCurrentBearing() {
        val current = _state.value.effectiveHeading
        _state.update {
            if (it.lockedBearing != null) {
                it.copy(lockedBearing = null)
            } else {
                it.copy(lockedBearing = current)
            }
        }
        triggerSubtleTick()
    }

    fun setTargetBearing(bearing: Float) {
        val normalized = (bearing % 360f + 360f) % 360f
        _state.update { it.copy(lockedBearing = normalized) }
        triggerSubtleTick()
    }

    fun clearLockedBearing() {
        _state.update { it.copy(lockedBearing = null) }
        triggerSubtleTick()
    }

    fun toggleManualDeclination() {
        _state.update { it.copy(manualDeclinationEnabled = !it.manualDeclinationEnabled) }
    }

    fun setManualDeclinationValue(value: Float) {
        _state.update { it.copy(manualDeclinationValue = value) }
    }

    fun updateLocationPermission(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            sensorManager.requestLocationUpdates()
        }
    }

    private fun checkHapticFeedback(state: CompassState) {
        if (!state.hapticFeedback || vibrator == null || !vibrator.hasVibrator()) return

        val now = System.currentTimeMillis()
        if (now - lastHapticTime < 350L) return

        val headingInt = state.effectiveHeading.roundToInt() % 360
        val isCardinal = headingInt == 0 || headingInt == 90 || headingInt == 180 || headingInt == 270

        val target = state.lockedBearing
        val isTargetLock = target != null && abs(state.effectiveHeading - target) < 1.5f

        if ((isCardinal || isTargetLock) && headingInt != lastHapticHeading) {
            lastHapticHeading = headingInt
            lastHapticTime = now
            triggerHapticPulse(if (isTargetLock || headingInt == 0) 40 else 20)
        }
    }

    private fun triggerSubtleTick() {
        if (_state.value.hapticFeedback) {
            triggerHapticPulse(25)
        }
    }

    private fun triggerHapticPulse(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
    }
}
