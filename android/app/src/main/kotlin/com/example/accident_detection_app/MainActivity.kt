package com.example.accident_detection_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "fall_detection_service"
    private val TAG = "MainActivity"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "startService" -> {
                        try {
                            // Check if sensor permission is granted (Android 10+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACTIVITY_RECOGNITION
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (!hasPermission) {
                                    Log.e(TAG, "ACTIVITY_RECOGNITION permission not granted")
                                    result.error(
                                        "PERMISSION_DENIED",
                                        "Activity Recognition permission required",
                                        null
                                    )
                                    return@setMethodCallHandler
                                }
                            }
                            
                            // Mark service as enabled in preferences
                            val prefs = getSharedPreferences("fall_detection_prefs", MODE_PRIVATE)
                            prefs.edit().putBoolean("service_enabled", true).apply()
                            
                            val intent = Intent(this, FallDetectionService::class.java)
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent)
                            } else {
                                startService(intent)
                            }
                            
                            Log.d(TAG, "Fall detection service started successfully")
                            result.success(true)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start service: ${e.message}", e)
                            result.error("SERVICE_ERROR", e.message, null)
                        }
                    }
                    
                    "stopService" -> {
                        try {
                            // Mark service as disabled
                            val prefs = getSharedPreferences("fall_detection_prefs", MODE_PRIVATE)
                            prefs.edit().putBoolean("service_enabled", false).apply()
                            
                            val intent = Intent(this, FallDetectionService::class.java)
                            stopService(intent)
                            
                            Log.d(TAG, "Service stopped")
                            result.success(true)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to stop service: ${e.message}", e)
                            result.error("SERVICE_ERROR", e.message, null)
                        }
                    }
                    
                    "isServiceRunning" -> {
                        result.success(FallDetectionService.isServiceRunning)
                    }
                    
                    else -> {
                        result.notImplemented()
                    }
                }
            }

        // Handle intent from notification
        intent?.getStringExtra("open_screen")?.let {
            if (it == "emergency") {
                flutterEngine.navigationChannel.pushRoute("/emergency")
            }
        }
    }
}