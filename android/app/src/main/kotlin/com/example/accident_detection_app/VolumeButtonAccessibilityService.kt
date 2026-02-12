package com.example.accident_detection_app

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.util.Log
import android.os.Build
import android.view.KeyEvent

class VolumeButtonAccessibilityService : AccessibilityService() { 

    companion object {
        private const val TAG = "VolumeAccessibility"
        private const val PRESS_WINDOW = 3000L // 3 seconds
        private const val REQUIRED_PRESSES = 3
    }

    private var pressCount = 0
    private var firstPressTime = 0L

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility service connected")
    }



    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP &&
            event.action == KeyEvent.ACTION_DOWN
        ) {
            handleVolumePress()
            return false
        }
        return false
    }

    private fun handleVolumePress() {
        val now = System.currentTimeMillis()

        if (pressCount == 0) {
            firstPressTime = now
        }

        // Reset if too much time passed
        if (now - firstPressTime > PRESS_WINDOW) {
            pressCount = 0
            firstPressTime = now
        }

        pressCount++
        Log.d(TAG, "Volume pressed: $pressCount")

        if (pressCount == REQUIRED_PRESSES) {
            triggerEmergency()
            pressCount = 0
        }
    }

    private fun triggerEmergency() {
        Log.w(TAG, "Emergency triggered via volume button")

        val intent = Intent(this, FallAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "emergency_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existingChannel = manager.getNotificationChannel(channelId)

            if (existingChannel == null) {
                    val channel = NotificationChannel(
                    channelId,
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical accident alerts"
                    enableVibration(true)
                    enableLights(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                manager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Emergency Triggered")
            .setContentText("Tap to respond")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(999, notification)

        Log.d(TAG, "Emergency notification shown from volume button")
    }




    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
