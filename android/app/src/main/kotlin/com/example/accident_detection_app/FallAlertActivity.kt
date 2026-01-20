package com.example.accident_detection_app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.app.NotificationManager
import androidx.core.content.ContextCompat
import android.util.Log

class FallAlertActivity : Activity() {

    private val SMS_PERMISSION_CODE = 101
    private val EMERGENCY_NUMBER = "7040040015"
    private val TAG = "FallAlertActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Show on lock screen (Samsung compatible)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                
                // Samsung specific flags
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }

            setContentView(R.layout.activity_fall_alert)

            findViewById<Button>(R.id.btnSendSms).setOnClickListener {
                dismissNotification()
                checkAndSendSms()
            }
            
            findViewById<Button>(R.id.btnImOk).setOnClickListener {
                dismissNotification()
                Toast.makeText(this, "Glad you're okay!", Toast.LENGTH_SHORT).show()
                finish()
            }
            
            Log.d(TAG, "FallAlertActivity created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            finish()
        }
    }

    private fun dismissNotification() {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(999)
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification: ${e.message}", e)
        }
    }

    private fun checkAndSendSms() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            sendSms()
        }
    }

    private fun sendSms() {
        try {
            // Use SmsManager compatible with all Android versions
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            val message = "EMERGENCY ALERT\nPossible accident detected. Please check immediately."

            // Send SMS
            smsManager.sendTextMessage(
                EMERGENCY_NUMBER,
                null,
                message,
                null,
                null
            )

            Toast.makeText(this, "Emergency SMS sent to $EMERGENCY_NUMBER", Toast.LENGTH_LONG).show()
            Log.d(TAG, "SMS sent successfully")
            
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}", e)
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            sendSms()
        } else {
            Toast.makeText(
                this,
                "SMS permission denied. Cannot send emergency message.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onDestroy() {
        // Ensure notification is dismissed
        dismissNotification()
        super.onDestroy()
    }
}