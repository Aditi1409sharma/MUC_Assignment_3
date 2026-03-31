package com.example.mucassignment3.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class BluetoothService(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private var socket: BluetoothSocket? = null
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _dataFlow = MutableStateFlow<String?>(null)
    val dataFlow = _dataFlow.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e("BT", "Bluetooth adapter not available or disabled")
            return@withContext false
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
            
            // Attempt 1: Standard Secure Socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            try {
                socket?.connect()
            } catch (e: IOException) {
                Log.w("BT", "Secure connection failed, trying insecure fallback...")
                // Attempt 2: Insecure Fallback (Often works better for HC-05)
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
            }

            _isConnected.value = true
            startReading()
            Log.d("BT", "Connected successfully to $address")
            true
        } catch (e: Exception) {
            Log.e("BT", "Connection failed: ${e.message}")
            _isConnected.value = false
            try { socket?.close() } catch (ce: Exception) {}
            false
        }
    }

    private fun startReading() {
        Thread {
            val inputStream: InputStream?
            try {
                inputStream = socket?.inputStream
            } catch (e: IOException) {
                _isConnected.value = false
                return@Thread
            }

            val buffer = ByteArray(1024)
            var bytes: Int

            while (_isConnected.value) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        _dataFlow.value = message
                    }
                } catch (e: IOException) {
                    Log.e("BT", "Disconnected during read")
                    _isConnected.value = false
                    break
                }
            }
        }.start()
    }

    fun disconnect() {
        _isConnected.value = false
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("BT", "Error closing socket")
        }
    }
}