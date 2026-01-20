// import 'dart:async';
// import 'dart:math';
// import 'dart:ui';

// import 'package:flutter_background_service/flutter_background_service.dart';
// import 'package:flutter_background_service_android/flutter_background_service_android.dart';
// import 'package:flutter_local_notifications/flutter_local_notifications.dart';
// import 'package:sensors_plus/sensors_plus.dart';
// import 'package:flutter/services.dart';

// const int EMERGENCY_NOTIFICATION_ID = 2001;

// Future<void> initializeFallService() async {
//   final service = FlutterBackgroundService();

//   await service.configure(
//     androidConfiguration: AndroidConfiguration(
//       onStart: onFallServiceStart,
//       autoStart: false,
//       isForegroundMode: true,

//       notificationChannelId: 'fall_service_channel',
//       initialNotificationTitle: 'Accident Detection Active',
//       initialNotificationContent: 'Monitoring device motion',
//       foregroundServiceNotificationId: 1001,
//     ),
//     iosConfiguration: IosConfiguration(
//       autoStart: false,
//       onForeground: onFallServiceStart,
//     ),
//   );
// }

// @pragma('vm:entry-point')
// void onFallServiceStart(ServiceInstance service) async {
//   DartPluginRegistrant.ensureInitialized();

//   bool fallDetected = false;
//   StreamSubscription? sub;

//   sub = accelerometerEvents.listen((event) {
//     final gForce = sqrt(
//       event.x * event.x + event.y * event.y + event.z * event.z,
//     );

//     if (gForce < 2 && !fallDetected) {
//       fallDetected = true;

//       _showEmergencyNotification();

//       Future.delayed(const Duration(seconds: 20), () {
//         fallDetected = false;
//       });
//     }
//   });

//   service.on('stop').listen((_) {
//     sub?.cancel();
//     service.stopSelf();
//   });
// }

// Future<void> _showEmergencyNotification() async {
//   final plugin = FlutterLocalNotificationsPlugin();

//   const androidDetails = AndroidNotificationDetails(
//     'emergency_channel',
//     'Emergency Alerts',
//     channelDescription: 'Accident detection alerts',
//     importance: Importance.max,
//     priority: Priority.high,
//     fullScreenIntent: true,
//     category: AndroidNotificationCategory.alarm,
//   );

//   await plugin.show(
//     EMERGENCY_NOTIFICATION_ID,
//     'Possible Accident Detected',
//     'Are you okay?',
//     const NotificationDetails(android: androidDetails),
//   );

//   // Open native full-screen activity
//   const platform = MethodChannel('fall_detection_service');
//   try {
//     await platform.invokeMethod('openEmergencyScreen');
//   } catch (_) {}
// }
