package com.example.accident_detection_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d(TAG, "Device booted or app updated, checking service status...")
            
            try {
                // Check if service was enabled before reboot
                val prefs = context.getSharedPreferences("fall_detection_prefs", Context.MODE_PRIVATE)
                val isServiceEnabled = prefs.getBoolean("service_enabled", false)
                
                if (isServiceEnabled) {
                    Log.d(TAG, "Auto-starting fall detection service...")
                    
                    val serviceIntent = Intent(context, FallDetectionService::class.java)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    Log.d(TAG, "Service auto-start completed")
                } else {
                    Log.d(TAG, "Service was not enabled, skipping auto-start")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-start service: ${e.message}", e)
            }
        }
    }
}