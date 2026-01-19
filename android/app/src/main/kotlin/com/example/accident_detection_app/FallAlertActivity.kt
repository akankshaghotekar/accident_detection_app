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
import androidx.core.content.ContextCompat

class FallAlertActivity : Activity() {

    private val SMS_PERMISSION_CODE = 101
    private val EMERGENCY_NUMBER = "7040040015"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_fall_alert)

        findViewById<Button>(R.id.btnSendSms).setOnClickListener {
            checkAndSendSms()
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
            val smsManager = SmsManager.getDefault()
            val message =
                "EMERGENCY ALERT \nPossible accident detected. Please check immediately."

            smsManager.sendTextMessage(
                EMERGENCY_NUMBER,
                null,
                message,
                null,
                null
            )

            Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_LONG).show()
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_LONG).show()
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
                "SMS permission denied",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
