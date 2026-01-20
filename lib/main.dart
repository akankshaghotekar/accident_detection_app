import 'package:accident_detection_app/permission/permission_gate.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(
    const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: PermissionGate(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static const platform = MethodChannel('fall_detection_service');

  Future<void> startService() async {
    try {
      await platform.invokeMethod('startService');
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    startService();
    return const MaterialApp(
      home: Scaffold(
        body: Center(
          child: Text(
            'Accident Detection Running',
            style: TextStyle(fontSize: 18),
          ),
        ),
      ),
    );
  }
}
