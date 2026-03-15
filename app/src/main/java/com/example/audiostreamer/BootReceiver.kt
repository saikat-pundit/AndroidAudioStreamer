package com.example.audiostreamer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AudioStreamer:BootWakeLock"
            )
            wakeLock.acquire(30000)
            
            try {
                Thread.sleep(5000) // Wait for system
                startAllServices(context)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
    
    private fun startAllServices(context: Context) {
        val audioIntent = Intent(context, AudioStreamService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(audioIntent)
        } else {
            context.startService(audioIntent)
        }
        
        val keepAliveIntent = Intent(context, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(keepAliveIntent)
        } else {
            context.startService(keepAliveIntent)
        }
        
        val heartbeatIntent = Intent(context, HeartbeatService::class.java)
        context.startService(heartbeatIntent)
    }
}
