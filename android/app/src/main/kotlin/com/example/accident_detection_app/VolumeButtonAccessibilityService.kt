package com.example.accident_detection_app

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
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

        val intent = Intent(this, FallDetectionService::class.java).apply {
            action = "TRIGGER_EMERGENCY"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }


    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
