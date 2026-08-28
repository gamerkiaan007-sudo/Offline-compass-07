package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CompassSensorManager(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _azimuthFlow = MutableStateFlow(0f)
    val azimuthFlow: StateFlow<Float> = _azimuthFlow.asStateFlow()

    private val _pitchFlow = MutableStateFlow(0f)
    val pitchFlow: StateFlow<Float> = _pitchFlow.asStateFlow()

    private val _rollFlow = MutableStateFlow(0f)
    val rollFlow: StateFlow<Float> = _rollFlow.asStateFlow()

    private val _magneticFieldFlow = MutableStateFlow(45f)
    val magneticFieldFlow: StateFlow<Float> = _magneticFieldFlow.asStateFlow()

    private val _accuracyFlow = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    val accuracyFlow: StateFlow<Int> = _accuracyFlow.asStateFlow()

    private val _declinationFlow = MutableStateFlow(0f)
    val declinationFlow: StateFlow<Float> = _declinationFlow.asStateFlow()

    private val _locationFlow = MutableStateFlow<Triple<Double?, Double?, Double?>>(Triple(null, null, null))
    val locationFlow: StateFlow<Triple<Double?, Double?, Double?>> = _locationFlow.asStateFlow()

    private val _isSensorAvailableFlow = MutableStateFlow(true)
    val isSensorAvailableFlow: StateFlow<Boolean> = _isSensorAvailableFlow.asStateFlow()

    // Smoothing filter buffers
    private var smoothedSin = 0f
    private var smoothedCos = 1f
    private val alpha = 0.15f // Smoothing factor for responsive yet steady dial

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startListening() {
        val hasRotation = rotationVectorSensor != null
        val hasAccMag = accelerometerSensor != null && magneticSensor != null

        if (!hasRotation && !hasAccMag) {
            _isSensorAvailableFlow.value = false
            return
        }
        _isSensorAvailableFlow.value = true

        if (hasRotation) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Also listen to magnetic sensor to get magnetic field strength even if rotation vector is used
        if (hasRotation && magneticSensor != null) {
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        requestLocationUpdates()
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        try {
            // Check last known location first for instantaneous offline declination
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val lastPassive = locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            val bestLast = listOfNotNull(lastGps, lastNetwork, lastPassive).maxByOrNull { it.time }
            bestLast?.let { updateGeomagneticDeclination(it) }

            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    5f,
                    this
                )
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    this
                )
            }
        } catch (_: SecurityException) {
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                processOrientation(orientationAngles)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                hasAccelerometer = true
                if (hasMagnetometer && rotationVectorSensor == null) {
                    computeOrientationFromAccMag()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometer = true
                val fieldStrength = sqrt(
                    event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]
                )
                _magneticFieldFlow.value = fieldStrength

                if (hasAccelerometer && rotationVectorSensor == null) {
                    computeOrientationFromAccMag()
                }
            }
        }
    }

    private fun computeOrientationFromAccMag() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            processOrientation(orientationAngles)
        }
    }

    private fun processOrientation(angles: FloatArray) {
        // angles[0] = Azimuth (-PI to PI)
        // angles[1] = Pitch (-PI/2 to PI/2)
        // angles[2] = Roll (-PI to PI)
        val rawAzimuthRad = angles[0]
        var rawDeg = Math.toDegrees(rawAzimuthRad.toDouble()).toFloat()
        rawDeg = (rawDeg % 360f + 360f) % 360f

        // Smooth filter using sin/cos to handle angle wrap-around seamlessly
        val curSin = sin(Math.toRadians(rawDeg.toDouble())).toFloat()
        val curCos = cos(Math.toRadians(rawDeg.toDouble())).toFloat()

        smoothedSin = alpha * curSin + (1f - alpha) * smoothedSin
        smoothedCos = alpha * curCos + (1f - alpha) * smoothedCos

        var smoothedDeg = Math.toDegrees(atan2(smoothedSin.toDouble(), smoothedCos.toDouble())).toFloat()
        smoothedDeg = (smoothedDeg % 360f + 360f) % 360f

        _azimuthFlow.value = smoothedDeg

        // Pitch & Roll in degrees
        val pitchDeg = Math.toDegrees(angles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(angles[2].toDouble()).toFloat()
        _pitchFlow.value = pitchDeg
        _rollFlow.value = rollDeg
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            _accuracyFlow.value = accuracy
        }
    }

    override fun onLocationChanged(location: Location) {
        updateGeomagneticDeclination(location)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun updateGeomagneticDeclination(location: Location) {
        _locationFlow.value = Triple(
            location.latitude,
            location.longitude,
            if (location.hasAltitude()) location.altitude else null
        )
        val geomagneticField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            location.time
        )
        _declinationFlow.value = geomagneticField.declination
    }
}
