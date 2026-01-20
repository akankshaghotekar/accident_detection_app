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
    await Permission.notification.request();
    await Permission.sms.request();
    await Permission.contacts.request();
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
          "please allow battery usage as 'Unrestricted'.",
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
