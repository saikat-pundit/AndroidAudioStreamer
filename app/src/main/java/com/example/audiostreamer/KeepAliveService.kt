package com.example.audiostreamer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class KeepAliveService : Service() {
    
    private val NOTIFICATION_ID = 1002
    private val CHANNEL_ID = "keep_alive_channel"
    private lateinit var wakeLock: PowerManager.WakeLock
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
        acquireWakeLock()
        startMonitoring()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Keep Alive Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps audio stream running"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Streamer")
            .setContentText("Background service active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AudioStreamer:KeepAlive"
        ).apply {
            acquire(10*60*1000L)
        }
    }
    
    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                delay(30000) // Check every 30 seconds
                
                // Check if main service is running
                if (!isAudioServiceRunning()) {
                    restartAudioService()
                }
                
                // Re-acquire wake lock
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(10*60*1000L)
                }
            }
        }
    }
    
    private fun isAudioServiceRunning(): Boolean {
        // Simplified check - actual implementation would use ActivityManager
        return true
    }
    
    private fun restartAudioService() {
        val intent = Intent(this, AudioStreamService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        // Restart
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
