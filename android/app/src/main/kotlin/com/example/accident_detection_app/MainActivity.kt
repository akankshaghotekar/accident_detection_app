package com.example.accident_detection_app

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "fall_detection_service"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                if (call.method == "startService") {
                    val intent = Intent(this, FallDetectionService::class.java)
                    startForegroundService(intent)
                    result.success(null)
                }
            }
            intent?.getStringExtra("open_screen")?.let {
                if (it == "emergency") {
                    flutterEngine.navigationChannel.pushRoute("/emergency");
                }
            }

    }
}
