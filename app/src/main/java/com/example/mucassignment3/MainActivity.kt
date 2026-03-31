package com.example.mucassignment3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.mucassignment3.bluetooth.BluetoothService
import com.example.mucassignment3.processor.DataProcessor
import com.example.mucassignment3.sensors.SensorHandler
import com.example.mucassignment3.ui.theme.MUCAssignment3Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothService: BluetoothService
    private lateinit var sensorHandler: SensorHandler
    private lateinit var dataProcessor: DataProcessor

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bluetoothService = BluetoothService(this)
        sensorHandler = SensorHandler(this)
        dataProcessor = DataProcessor(this)

        checkPermissions()

        setContent {
            MUCAssignment3Theme {
                var showConsent by remember { mutableStateOf(true) }
                
                if (showConsent) {
                    ConsentDialog(onDismiss = { showConsent = false })
                } else {
                    SafetyBubbleApp(bluetoothService, sensorHandler, dataProcessor)
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.VIBRATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
        sensorHandler.stop()
        dataProcessor.stopProcessing()
        dataProcessor.release()
    }
}

@Composable
fun ConsentDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF3498DB),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Informed Consent",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = "This application collects environmental data (distance, light) and physical movement data (phone sensors) to provide safety alerts. \n\nAll data is stored LOCALLY on this device and is not transmitted to any server. By clicking 'I Agree', you consent to the collection and local storage of this data for research and assistive purposes.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I AGREE")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyBubbleApp(
    bluetoothService: BluetoothService,
    sensorHandler: SensorHandler,
    dataProcessor: DataProcessor
) {
    val isConnected by bluetoothService.isConnected.collectAsState()
    val bluetoothData by bluetoothService.dataFlow.collectAsState()
    val accelData by sensorHandler.accelData.collectAsState()
    val gyroData by sensorHandler.gyroData.collectAsState()
    val currentState by dataProcessor.currentState.collectAsState()

    var deviceAddress by remember { mutableStateOf("") }
    var voiceEnabled by remember { mutableStateOf(true) }
    var distThreshold by remember { mutableStateOf(50f) }
    var lightThreshold by remember { mutableStateOf(80f) }

    var distance by remember { mutableStateOf(0f) }
    var light by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Background color animation based on state
    val backgroundColor by animateColorAsState(
        targetValue = when (currentState) {
            "Too Close" -> Color(0xFFFFEBEE)
            "Harsh Light" -> Color(0xFFFFF3E0)
            "Movement Alert" -> Color(0xFFF3E5F5)
            "Overstimulated" -> Color(0xFFFFCDD2)
            else -> Color(0xFFE8F5E9)
        },
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        sensorHandler.start()
        dataProcessor.startProcessing()
    }

    LaunchedEffect(distThreshold, lightThreshold) {
        dataProcessor.distanceThreshold = distThreshold
        dataProcessor.lightThreshold = lightThreshold.toInt()
    }

    LaunchedEffect(bluetoothData) {
        bluetoothData?.let { data ->
            try {
                val parts = data.trim().split(",")
                if (parts.size >= 2) {
                    distance = parts[0].toFloatOrNull() ?: distance
                    light = parts[1].toIntOrNull() ?: light
                    dataProcessor.addDataPoint(accelData, gyroData, distance, light)
                }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security, 
                            contentDescription = null,
                            tint = Color(0xFF2C3E50),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Safety Bubble", 
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = Color(0xFF2C3E50),
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Connection Status Section
            ConnectionCard(
                isConnected = isConnected,
                deviceAddress = deviceAddress,
                onAddressChange = { deviceAddress = it },
                onConnect = { scope.launch { bluetoothService.connect(deviceAddress) } }
            )

            // Main State Visualizer
            StateVisualizer(currentState = currentState)

            // Live Readings Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorDataCard(
                    modifier = Modifier.weight(1f),
                    label = "Distance",
                    value = "${distance.toInt()} cm",
                    icon = Icons.Default.GraphicEq,
                    color = Color(0xFF3498DB)
                )
                SensorDataCard(
                    modifier = Modifier.weight(1f),
                    label = "Ambient Light",
                    value = "$light",
                    icon = Icons.Default.LightMode,
                    color = Color(0xFFF1C40F)
                )
            }

            // Controls Section
            SettingsCard(
                distThreshold = distThreshold,
                onDistChange = { distThreshold = it },
                lightThreshold = lightThreshold,
                onLightChange = { lightThreshold = it },
                voiceEnabled = voiceEnabled,
                onVoiceToggle = { 
                    voiceEnabled = it
                    dataProcessor.setVoiceEnabled(it)
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ConnectionCard(
    isConnected: Boolean,
    deviceAddress: String,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) Color(0xFF27AE60) else Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "System Online" else "Hardware Offline",
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF27AE60) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = deviceAddress,
                onValueChange = onAddressChange,
                label = { Text("HC-05 MAC Address") },
                placeholder = { Text("98:D3:31:F4:12:34") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFF27AE60) else Color(0xFF3498DB)
                )
            ) {
                Text(if (isConnected) "Reconnect Hardware" else "Link Hardware")
            }
        }
    }
}

@Composable
fun StateVisualizer(currentState: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Pulse effect could be added here
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = when (currentState) {
                    "Too Close" -> Color(0xFFE74C3C)
                    "Harsh Light" -> Color(0xFFF39C12)
                    "Movement Alert" -> Color(0xFF9B59B6)
                    "Overstimulated" -> Color(0xFFC0392B)
                    else -> Color(0xFF2ECC71)
                },
                shadowElevation = 8.dp
            ) {}
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = currentState.uppercase(),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF2C3E50)
        )
    }
}

@Composable
fun SensorDataCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun SettingsCard(
    distThreshold: Float,
    onDistChange: (Float) -> Unit,
    lightThreshold: Float,
    onLightChange: (Float) -> Unit,
    voiceEnabled: Boolean,
    onVoiceToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Personalization", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Distance Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Slider(value = distThreshold, onValueChange = onDistChange, valueRange = 20f..200f)
            Text("${distThreshold.toInt()} cm", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))

            Spacer(modifier = Modifier.height(12.dp))

            Text("Light Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Slider(value = lightThreshold, onValueChange = onLightChange, valueRange = 1f..100f)
            Text("${lightThreshold.toInt()}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))

            Divider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voice Assistance", modifier = Modifier.weight(1f), fontSize = 14.sp)
                Switch(checked = voiceEnabled, onCheckedChange = onVoiceToggle)
            }
        }
    }
}