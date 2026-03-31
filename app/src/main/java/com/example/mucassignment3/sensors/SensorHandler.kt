package com.example.mucassignment3.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class SensorHandler(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _accelData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val accelData = _accelData.asStateFlow()

    private val _gyroData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroData = _gyroData.asStateFlow()

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                _accelData.value = event.values.copyOf()
            }
            Sensor.TYPE_GYROSCOPE -> {
                _gyroData.value = event.values.copyOf()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getMagnitude(values: FloatArray): Float {
        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }
}