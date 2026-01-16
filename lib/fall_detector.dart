import 'dart:async';
import 'dart:math';
import 'package:sensors_plus/sensors_plus.dart';
import 'notification_service.dart';

class FallDetector {
  StreamSubscription? _sub;
  bool _fallDetected = false;

  void start() {
    _sub = accelerometerEvents.listen((event) {
      final double gForce = sqrt(
        event.x * event.x + event.y * event.y + event.z * event.z,
      );

      // Normal gravity ≈ 9.8
      // Free fall ≈ < 2
      if (gForce < 2 && !_fallDetected) {
        _fallDetected = true;
        NotificationService.showEmergencyNotification();

        // Prevent spam
        Future.delayed(const Duration(seconds: 20), () {
          _fallDetected = false;
        });
      }
    });
  }

  void stop() {
    _sub?.cancel();
  }
}
