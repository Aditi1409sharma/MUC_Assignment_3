# Adaptive Personal Safety Bubble for Sensory Sensitivity

An assistive Android application that connects to an Arduino device via Bluetooth (HC-05) and uses smartphone sensors to detect and alert users of environmental stressors.

## 🎯 Project Goal
The app helps users (especially with sensory sensitivity or autism) by:
1. Detecting when objects are too close (via ultrasonic sensor data).
2. Detecting harsh lighting conditions (via LDR).
3. Detecting user movement and discomfort using phone sensors.
4. Providing multimodal feedback using vibration patterns and voice alerts.

## 📡 Features
- **Bluetooth Connectivity**: Connects to HC-05 to receive real-time distance and light data.
- **Sensor Fusion**: Combines Arduino data with phone Accelerometer and Gyroscope data.
- **Dynamic Thresholds**: Adjust sensitivity for light and distance via UI sliders.
- **Multimodal Feedback**: 
  - **Visual**: Dynamic background colors and state visualizer.
  - **Haptic**: Unique vibration patterns for different triggers.
  - **Auditory**: Text-to-Speech alerts.
- **Data Logging**: Stores safety events locally using Room Database.

## 🛠️ Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: Coroutines, StateFlow, Room DB
- **API Level**: Compile/Target SDK 36 (Android 15)

## 🔧 Hardware Setup
- Arduino (Uno/Nano)
- HC-05 Bluetooth Module
- HC-SR04 Ultrasonic Sensor
- LDR (Photoresistor)
- Connect HC-05 TX to Arduino RX (Pin 0) and HC-05 RX to Arduino TX (Pin 1).

## 🚀 How to Use
1. Pair your HC-05 module in Android Bluetooth settings.
2. Launch the app and enter the HC-05 MAC address.
3. Click "Link Hardware" to connect.
4. Adjust sensitivity sliders as needed.
5. The system automatically monitors and alerts based on the current environment.
