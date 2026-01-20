package com.example.accident_detection_app

import android.app.*
import android.content.Intent
import android.hardware.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import android.util.Log
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var fallDetected = false
    
    companion object {
        private const val TAG = "FallDetectionService"
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            
            if (accelerometer == null) {
                Log.e(TAG, "Accelerometer not available on this device")
                stopSelf()
                return
            }
            
            val registered = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            
            if (!registered) {
                Log.e(TAG, "Failed to register sensor listener")
                stopSelf()
                return
            }
            
            Log.d(TAG, "Sensor registered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = createPersistentNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+ (API 34)
                    startForeground(
                        1,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                } else {
                    // Android 10-13 (API 29-33)
                    startForeground(
                        1,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                }
            } else {
                // Android 9 and below
                startForeground(1, notification)
            }
            
            Log.d(TAG, "Foreground service started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun createPersistentNotification(): Notification {
        val channelId = "fall_detection_service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = getSystemService(NotificationManager::class.java)
                .getNotificationChannel(channelId)
                
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Accident Detection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Monitors device motion for safety"
                    setShowBadge(false)
                }
                
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Accident Detection Active")
            .setContentText("Monitoring device motion for safety")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent) {
        try {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = sqrt(x * x + y * y + z * z)

            // REALISTIC FALL LOGIC (Free fall detection)
            if (gForce < 2.0f && !fallDetected) {
                fallDetected = true
                Log.w(TAG, "Fall detected! G-Force: $gForce")
                
                showEmergencyNotification()

                // Reset after 20 seconds
                Thread {
                    try {
                        Thread.sleep(20000)
                        fallDetected = false
                        Log.d(TAG, "Fall detection reset")
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "Sleep interrupted", e)
                    }
                }.start()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSensorChanged: ${e.message}", e)
        }
    }

    private fun showEmergencyNotification() {
        try {
            val channelId = "emergency_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existingChannel = getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(channelId)
                    
                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        channelId,
                        "Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Critical accident alerts"
                        setShowBadge(true)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 250, 500)
                    }
                    
                    getSystemService(NotificationManager::class.java)
                        .createNotificationChannel(channel)
                }
            }

            val intent = Intent(this, FallAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Possible Accident Detected")
                .setContentText("Tap to respond")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .build()

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(999, notification)
            
            Log.d(TAG, "Emergency notification shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    override fun onDestroy() {
        try {
            sensorManager.unregisterListener(this)
            Log.d(TAG, "Service destroyed, sensor unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}