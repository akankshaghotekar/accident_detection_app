import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HomeLauncher extends StatelessWidget {
  const HomeLauncher({super.key});

  static const MethodChannel _channel = MethodChannel('fall_detection_service');

  Future<void> _startService() async {
    try {
      await _channel.invokeMethod('startService');
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    _startService();

    return const Scaffold(
      body: Center(
        child: Text(
          'Accident Detection Running',
          style: TextStyle(fontSize: 18),
        ),
      ),
    );
  }
}
