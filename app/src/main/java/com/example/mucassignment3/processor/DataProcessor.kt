package com.example.mucassignment3.processor

import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import com.example.mucassignment3.data.AppDatabase
import com.example.mucassignment3.data.SafetyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class DataProcessor(private val context: Context) : TextToSpeech.OnInitListener {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private val database = AppDatabase.getDatabase(context)

    private val _currentState = MutableStateFlow("Calm")
    val currentState = _currentState.asStateFlow()

    private var voiceEnabled = true

    // Thresholds
    var distanceThreshold = 50f
    var lightThreshold = 80 // Updated for 1-100 range

    private var processingJob: Job? = null

    // For feature extraction
    private val accelHistory = mutableListOf<Float>()
    private val gyroHistory = mutableListOf<Float>()
    private val distanceHistory = mutableListOf<Float>()
    private val lightHistory = mutableListOf<Int>()

    fun setVoiceEnabled(enabled: Boolean) {
        voiceEnabled = enabled
    }

    fun addDataPoint(accel: FloatArray, gyro: FloatArray, distance: Float, light: Int) {
        accelHistory.add(sqrt(accel[0]*accel[0] + accel[1]*accel[1] + accel[2]*accel[2]))
        gyroHistory.add(sqrt(gyro[0]*gyro[0] + gyro[1]*gyro[1] + gyro[2]*gyro[2]))
        distanceHistory.add(distance)
        lightHistory.add(light)
    }

    fun startProcessing() {
        processingJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(2000)
                processFeatures()
            }
        }
    }

    fun stopProcessing() {
        processingJob?.cancel()
    }

    private suspend fun processFeatures() {
        if (distanceHistory.isEmpty()) return

        val avgDistance = distanceHistory.average().toFloat()
        val avgLight = lightHistory.average().toInt()
        val accelVar = calculateVariance(accelHistory)
        val gyroMax = gyroHistory.maxOrNull() ?: 0f

        val activeIssues = mutableListOf<String>()

        // 1. Distance Outcome (Ultrasonic)
        if (avgDistance < distanceThreshold) {
            activeIssues.add("Too Close")
            vibratePattern(longArrayOf(0, 200, 100, 200)) // Double pulse
            speak("Object nearby")
        }

        // 2. Light Outcome (LDR)
        if (avgLight > lightThreshold) {
            activeIssues.add("Harsh Light")
            vibratePattern(longArrayOf(0, 500)) // Strong single pulse
            speak("Lighting too bright")
        }

        // 3. Movement Outcome (Sensors)
        if (accelVar > 12.0 || gyroMax > 6.0) {
            activeIssues.add("Movement Alert")
            vibratePattern(longArrayOf(0, 100, 50, 100, 50, 100)) // Triple rapid pulse
            speak("Sudden movement detected")
        }

        // 4. Combined Complex Outcome
        val newState = if (activeIssues.isEmpty()) {
            "Calm"
        } else if (activeIssues.size >= 2) {
            "Overstimulated"
        } else {
            activeIssues.first()
        }

        if (newState != _currentState.value) {
            _currentState.value = newState
            if (newState == "Overstimulated") {
                vibratePattern(longArrayOf(0, 1000, 200, 1000)) // Two very long pulses
                speak("High stimulation environment")
            }
            saveEvent(avgDistance, avgLight, newState)
        }

        // Clear history for next window
        accelHistory.clear()
        gyroHistory.clear()
        distanceHistory.clear()
        lightHistory.clear()
    }

    private fun calculateVariance(list: List<Float>): Double {
        if (list.isEmpty()) return 0.0
        val mean = list.average()
        return list.map { (it - mean) * (it - mean) }.average()
    }

    private fun vibratePattern(pattern: LongArray) {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun speak(text: String) {
        if (voiceEnabled) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private suspend fun saveEvent(distance: Float, light: Int, state: String) {
        database.safetyDao().insertEvent(
            SafetyEvent(
                timestamp = System.currentTimeMillis(),
                distance = distance,
                light = light,
                state = state
            )
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
    }
}