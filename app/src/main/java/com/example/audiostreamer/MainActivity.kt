package com.example.audiostreamer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var ipAddressInput: EditText
    private lateinit var portInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        ipAddressInput = findViewById(R.id.ipAddressInput)
        portInput = findViewById(R.id.portInput)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        
        // Set default values
        ipAddressInput.setText("192.168.1.100") // Change this to your Ubuntu IP
        portInput.setText("8080")
        
        // Set click listeners
        startButton.setOnClickListener {
            startStreaming()
        }
        
        stopButton.setOnClickListener {
            stopStreaming()
        }
        
        // Check if service is already running
        updateStatus(false)
    }
    
    private fun startStreaming() {
        val ipAddress = ipAddressInput.text.toString()
        val port = portInput.text.toString().toIntOrNull() ?: 8080
        
        if (ipAddress.isEmpty()) {
            Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Start foreground service
        val intent = Intent(this, AudioStreamService::class.java).apply {
            putExtra("IP_ADDRESS", ipAddress)
            putExtra("PORT", port)
            action = "START_STREAMING"
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        updateStatus(true)
        Toast.makeText(this, "Streaming started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopStreaming() {
        val intent = Intent(this, AudioStreamService::class.java).apply {
            action = "STOP_STREAMING"
        }
        startService(intent)
        
        updateStatus(false)
        Toast.makeText(this, "Streaming stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateStatus(isStreaming: Boolean) {
        if (isStreaming) {
            statusText.text = "Status: Streaming Active"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            startButton.isEnabled = false
            stopButton.isEnabled = true
        } else {
            statusText.text = "Status: Not Streaming"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }
}
