package com.example.audiostreamer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.Socket

class AudioStreamService : Service() {
    
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "audio_stream_channel"
    
    private var audioRecord: AudioRecord? = null
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var isStreaming = false
    private lateinit var wakeLock: PowerManager.WakeLock
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Audio configuration
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, 
        CHANNEL_CONFIG, 
        AUDIO_FORMAT
    ) * 4
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AudioStreamer:WakeLock"
        ).apply {
            acquire(10*60*1000L) // 10 minutes
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Stream Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Streams audio to desktop"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Streamer")
            .setContentText("Streaming audio to desktop...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_STREAMING" -> {
                val ipAddress = intent.getStringExtra("IP_ADDRESS") ?: "192.168.1.100"
                val port = intent.getIntExtra("PORT", 8080)
                startForeground(NOTIFICATION_ID, createNotification())
                startStreaming(ipAddress, port)
            }
            "STOP_STREAMING" -> {
                stopStreaming()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    private fun startStreaming(ipAddress: String, port: Int) {
        if (isStreaming) return
        
        isStreaming = true
        
        serviceScope.launch {
            try {
                Log.d("AudioStream", "Connecting to $ipAddress:$port")
                
                // Connect to desktop
                socket = Socket(ipAddress, port)
                outputStream = socket?.getOutputStream()
                
                Log.d("AudioStream", "Connected successfully")
                
                // Initialize AudioRecord with REMOTE_SUBMIX for system audio
                audioRecord = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.REMOTE_SUBMIX,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        BUFFER_SIZE
                    )
                } catch (e: SecurityException) {
                    // Fallback to MIC if REMOTE_SUBMIX fails
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        BUFFER_SIZE
                    )
                }
                
                audioRecord?.startRecording()
                
                val buffer = ByteArray(BUFFER_SIZE)
                
                while (isStreaming && socket?.isConnected == true) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        try {
                            outputStream?.write(buffer, 0, bytesRead)
                            outputStream?.flush()
                        } catch (e: Exception) {
                            Log.e("AudioStream", "Error sending data", e)
                            break
                        }
                    }
                    
                    // Keep wake lock alive
                    if (!wakeLock.isHeld) {
                        wakeLock.acquire(10*60*1000L)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("AudioStream", "Streaming error", e)
            } finally {
                cleanup()
                
                // Try to reconnect after delay
                if (isStreaming) {
                    delay(5000)
                    startStreaming(ipAddress, port)
                }
            }
        }
    }
    
    private fun stopStreaming() {
        isStreaming = false
        cleanup()
        serviceScope.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    
    private fun cleanup() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        outputStream = null
        socket = null
    }
    
    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
