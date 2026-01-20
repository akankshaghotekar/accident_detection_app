import 'dart:io';
import 'package:accident_detection_app/home_launcher.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class PermissionGate extends StatefulWidget {
  const PermissionGate({super.key});

  @override
  State<PermissionGate> createState() => _PermissionGateState();
}

class _PermissionGateState extends State<PermissionGate> {
  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _requestBasicPermissions();
      await _handleBatteryOptimization();
      _goToHome();
    });
  }

  Future<void> _requestBasicPermissions() async {
    // Notification permission (Android 13+)
    await Permission.notification.request();

    // SMS permission
    await Permission.sms.request();

    // Contacts permission
    await Permission.contacts.request();

    // CRITICAL: Activity Recognition (Android 10+)
    // Required for accelerometer access in background
    if (Platform.isAndroid) {
      final status = await Permission.activityRecognition.status;

      if (!status.isGranted) {
        final result = await Permission.activityRecognition.request();

        if (!result.isGranted && mounted) {
          _showPermissionDeniedDialog(
            "Motion Sensor Access Required",
            "This app needs motion sensor permission to detect accidents. "
                "Please grant 'Physical Activity' permission in settings.",
          );
        }
      }
    }

    // Sensors permission (Android 12+)
    if (Platform.isAndroid) {
      await Permission.sensors.request();
    }
  }

  Future<void> _handleBatteryOptimization() async {
    if (!Platform.isAndroid) return;

    final isIgnored = await Permission.ignoreBatteryOptimizations.isGranted;

    if (isIgnored) return;

    if (!mounted) return;

    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        title: const Text("Allow Battery Permission"),
        content: const Text(
          "For accident detection to work properly in background, "
          "please allow battery usage as 'Unrestricted'.\n\n"
          "Samsung users: Settings → Battery → Background usage limits → "
          "Add this app to 'Never sleeping apps'",
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Later"),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              try {
                await Permission.ignoreBatteryOptimizations.request();
              } catch (e) {
                debugPrint("Battery optimization request failed: $e");
              }
            },
            child: const Text("Open Settings"),
          ),
        ],
      ),
    );
  }

  void _showPermissionDeniedDialog(String title, String message) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Cancel"),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              openAppSettings();
            },
            child: const Text("Open Settings"),
          ),
        ],
      ),
    );
  }

  void _goToHome() {
    if (!mounted) return;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (_) => const HomeLauncher()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}
