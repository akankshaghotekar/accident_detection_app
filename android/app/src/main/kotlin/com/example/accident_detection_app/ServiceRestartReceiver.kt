package com.example.accident_detection_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ServiceRestartReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ServiceRestartReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received restart broadcast")
        
        try {
            // Check if service should be running
            val prefs = context.getSharedPreferences("fall_detection_prefs", Context.MODE_PRIVATE)
            val isServiceEnabled = prefs.getBoolean("service_enabled", false)
            
            if (!isServiceEnabled) {
                Log.d(TAG, "Service is disabled, not restarting")
                return
            }
            
            // Check if service is already running
            if (FallDetectionService.isServiceRunning) {
                Log.d(TAG, "Service is already running, no restart needed")
                return
            }
            
            Log.d(TAG, "Service not running but should be - restarting...")
            
            val serviceIntent = Intent(context, FallDetectionService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "Service restart initiated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart service: ${e.message}", e)
        }
    }
}