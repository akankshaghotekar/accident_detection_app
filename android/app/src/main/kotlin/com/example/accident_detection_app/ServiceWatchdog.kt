package com.example.accident_detection_app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object ServiceWatchdog {
    
    private const val TAG = "ServiceWatchdog"
    private const val WATCHDOG_INTERVAL = 5 * 60 * 1000L // 5 minutes
    
    fun scheduleWatchdog(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent("com.example.accident_detection_app.RESTART_SERVICE")
            intent.setPackage(context.packageName)
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val triggerTime = System.currentTimeMillis() + WATCHDOG_INTERVAL
            
            // Use inexact alarm (doesn't need special permission)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                WATCHDOG_INTERVAL,
                pendingIntent
            )
            
            Log.d(TAG, "Watchdog scheduled for service health check")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule watchdog: ${e.message}", e)
        }
    }
    
    fun cancelWatchdog(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent("com.example.accident_detection_app.RESTART_SERVICE")
            intent.setPackage(context.packageName)
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            
            Log.d(TAG, "Watchdog cancelled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel watchdog: ${e.message}", e)
        }
    }
}