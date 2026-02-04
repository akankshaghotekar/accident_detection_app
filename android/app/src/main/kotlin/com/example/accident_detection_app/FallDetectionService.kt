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
    private var gyroscope: Sensor? = null
    
    // Fall detection state machine
    private enum class FallState {
        NORMAL, FREE_FALL_DETECTED, IMPACT_DETECTED, STATIONARY_CHECK
    }
    
    private var currentState = FallState.NORMAL
    private var freeFallStartTime = 0L
    private var impactTime = 0L
    private var stationaryCheckStartTime = 0L
    
    // Thresholds and parameters
    private val FREE_FALL_THRESHOLD = 5.0f // m/s² (less than normal gravity)
    private val IMPACT_THRESHOLD = 25.0f // m/s² (strong impact)
    private val STATIONARY_THRESHOLD = 2.0f // m/s² (minimal movement)
    
    private val MIN_FREE_FALL_DURATION = 300L // milliseconds
    private val MAX_FREE_FALL_DURATION = 1500L // milliseconds
    private val IMPACT_WINDOW = 500L // milliseconds after free fall
    private val STATIONARY_CHECK_DURATION = 2000L // 2 seconds
    
    // Movement tracking
    private val recentAccelerations = mutableListOf<Float>()
    private val ACCELERATION_HISTORY_SIZE = 30 // ~1 second at SENSOR_DELAY_NORMAL
    
    // Angular velocity tracking (gyroscope)
    private var angularVelocityMagnitude = 0f
    private val HIGH_ROTATION_THRESHOLD = 3.0f // rad/s
    
    // False positive prevention
    private var lastFallAlertTime = 0L
    private val MIN_TIME_BETWEEN_ALERTS = 30000L // 30 seconds
    
    companion object {
        private const val TAG = "FallDetectionService"
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        
        isServiceRunning = true
        
        val prefs = getSharedPreferences("fall_detection_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", true).apply()
        
        try {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            
            if (accelerometer == null) {
                Log.e(TAG, "Accelerometer not available")
                stopSelf()
                return
            }
            
            // Register accelerometer
            val accRegistered = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            
            // Register gyroscope (optional but helpful)
            if (gyroscope != null) {
                sensorManager.registerListener(
                    this,
                    gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                Log.d(TAG, "Gyroscope registered")
            } else {
                Log.w(TAG, "Gyroscope not available, using accelerometer only")
            }
            
            if (!accRegistered) {
                Log.e(TAG, "Failed to register accelerometer")
                stopSelf()
                return
            }
            
            Log.d(TAG, "Sensors registered successfully")
            
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
                    startForeground(
                        1,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                } else {
                    startForeground(
                        1,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                }
            } else {
                startForeground(1, notification)
            }
            
            ServiceWatchdog.scheduleWatchdog(this)
            
            Log.d(TAG, "Foreground service started with watchdog")
            
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
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
                Sensor.TYPE_GYROSCOPE -> handleGyroscope(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSensorChanged: ${e.message}", e)
        }
    }
    
    private fun handleGyroscope(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Calculate angular velocity magnitude
        angularVelocityMagnitude = sqrt(x * x + y * y + z * z)
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z)
        
        // Maintain acceleration history
        recentAccelerations.add(acceleration)
        if (recentAccelerations.size > ACCELERATION_HISTORY_SIZE) {
            recentAccelerations.removeAt(0)
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Prevent duplicate alerts
        if (currentTime - lastFallAlertTime < MIN_TIME_BETWEEN_ALERTS) {
            return
        }
        
        // State machine for fall detection
        when (currentState) {
            FallState.NORMAL -> {
                // Look for free fall (low g-force)
                if (acceleration < FREE_FALL_THRESHOLD) {
                    currentState = FallState.FREE_FALL_DETECTED
                    freeFallStartTime = currentTime
                    Log.d(TAG, "Free fall detected: ${acceleration}m/s²")
                }
            }
            
            FallState.FREE_FALL_DETECTED -> {
                val freeFallDuration = currentTime - freeFallStartTime
                
                // Check if free fall duration is valid
                if (freeFallDuration > MAX_FREE_FALL_DURATION) {
                    // Too long, probably not a fall
                    resetFallDetection()
                    Log.d(TAG, "Free fall too long, resetting")
                } else if (acceleration > IMPACT_THRESHOLD) {
                    // Strong impact detected after free fall
                    if (freeFallDuration >= MIN_FREE_FALL_DURATION) {
                        currentState = FallState.IMPACT_DETECTED
                        impactTime = currentTime
                        Log.w(TAG, "Impact detected: ${acceleration}m/s² after ${freeFallDuration}ms free fall")
                    } else {
                        // Impact too soon, probably just phone movement
                        resetFallDetection()
                        Log.d(TAG, "Impact too soon after free fall, resetting")
                    }
                } else if (acceleration > FREE_FALL_THRESHOLD && freeFallDuration < MIN_FREE_FALL_DURATION) {
                    // Free fall ended too quickly, not a real fall
                    resetFallDetection()
                }
            }
            
            FallState.IMPACT_DETECTED -> {
                // Start stationary check
                currentState = FallState.STATIONARY_CHECK
                stationaryCheckStartTime = currentTime
                Log.d(TAG, "Starting stationary check")
            }
            
            FallState.STATIONARY_CHECK -> {
                val stationaryDuration = currentTime - stationaryCheckStartTime
                
                if (stationaryDuration >= STATIONARY_CHECK_DURATION) {
                    // Check if device has been relatively stationary
                    if (isDeviceStationary()) {
                        // Confirm fall - all conditions met
                        confirmFall()
                    } else {
                        Log.d(TAG, "Device not stationary after impact, likely false positive")
                        resetFallDetection()
                    }
                }
            }
        }
    }
    
    private fun isDeviceStationary(): Boolean {
        if (recentAccelerations.size < 10) return false
        
        // Calculate standard deviation of recent accelerations
        val mean = recentAccelerations.takeLast(10).average()
        val variance = recentAccelerations.takeLast(10)
            .map { (it - mean) * (it - mean) }
            .average()
        val stdDev = sqrt(variance.toFloat())
        
        // Check if movement is minimal
        val isStationary = stdDev < STATIONARY_THRESHOLD
        
        // Also check gyroscope if available
        val notRotating = angularVelocityMagnitude < HIGH_ROTATION_THRESHOLD
        
        Log.d(TAG, "Stationary check - StdDev: $stdDev, AngularVel: $angularVelocityMagnitude")
        
        return isStationary && notRotating
    }
    
    private fun confirmFall() {
        Log.w(TAG, "FALL CONFIRMED - Showing emergency notification")
        lastFallAlertTime = System.currentTimeMillis()
        showEmergencyNotification()
        resetFallDetection()
    }
    
    private fun resetFallDetection() {
        currentState = FallState.NORMAL
        freeFallStartTime = 0L
        impactTime = 0L
        stationaryCheckStartTime = 0L
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
            isServiceRunning = false
            Log.d(TAG, "Service destroyed, sensor unregistered")
            
            val prefs = getSharedPreferences("fall_detection_prefs", MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("service_enabled", false)
            
            if (isEnabled) {
                Log.d(TAG, "Service was force-killed, scheduling restart...")
                scheduleServiceRestart()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
        super.onDestroy()
    }
    
    private fun scheduleServiceRestart() {
        try {
            val restartIntent = Intent("com.example.accident_detection_app.RESTART_SERVICE")
            restartIntent.setPackage(packageName)
            sendBroadcast(restartIntent)
            
            Log.d(TAG, "Restart broadcast sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart: ${e.message}", e)
        }
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        Log.w(TAG, "Task removed from recent apps")
        scheduleServiceRestart()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}