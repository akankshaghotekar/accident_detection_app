import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HomeLauncher extends StatefulWidget {
  const HomeLauncher({super.key});

  @override
  State<HomeLauncher> createState() => _HomeLauncherState();
}

class _HomeLauncherState extends State<HomeLauncher>
    with WidgetsBindingObserver {
  static const MethodChannel _channel = MethodChannel('fall_detection_service');

  bool _isServiceRunning = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _startService();
    _checkServiceStatus();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      // Check service status when app comes to foreground
      _checkServiceStatus();
    }
  }

  Future<void> _startService() async {
    try {
      final result = await _channel.invokeMethod('startService');
      debugPrint('Service start result: $result');
      await _checkServiceStatus();
    } catch (e) {
      debugPrint('Failed to start service: $e');
    }
  }

  Future<void> _checkServiceStatus() async {
    try {
      final isRunning = await _channel.invokeMethod('isServiceRunning');
      setState(() {
        _isServiceRunning = isRunning ?? false;
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('Failed to check service status: $e');
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _stopService() async {
    try {
      await _channel.invokeMethod('stopService');
      await _checkServiceStatus();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Fall detection stopped'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      debugPrint('Failed to stop service: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Accident Detection'),
        centerTitle: true,
        backgroundColor: Colors.red.shade700,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Center(
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Status Icon
                    Icon(
                      _isServiceRunning ? Icons.shield : Icons.shield_outlined,
                      size: 100,
                      color: _isServiceRunning ? Colors.green : Colors.grey,
                    ),

                    const SizedBox(height: 24),

                    // Status Text
                    Text(
                      _isServiceRunning
                          ? 'Fall Detection Active'
                          : 'Fall Detection Inactive',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: _isServiceRunning ? Colors.green : Colors.grey,
                      ),
                    ),

                    const SizedBox(height: 16),

                    // Description
                    Text(
                      _isServiceRunning
                          ? 'Continuously monitoring for falls\nEven when app is closed'
                          : 'Service has been stopped',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.grey.shade600,
                      ),
                    ),

                    const SizedBox(height: 48),

                    // Refresh Button
                    ElevatedButton.icon(
                      onPressed: _checkServiceStatus,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Refresh Status'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 32,
                          vertical: 16,
                        ),
                      ),
                    ),

                    const SizedBox(height: 16),

                    // Stop Button (only show if running)
                    if (_isServiceRunning)
                      OutlinedButton.icon(
                        onPressed: () async {
                          final confirm = await showDialog<bool>(
                            context: context,
                            builder: (_) => AlertDialog(
                              title: const Text('Stop Fall Detection?'),
                              content: const Text(
                                'Are you sure you want to stop fall detection? '
                                'You will not be protected.',
                              ),
                              actions: [
                                TextButton(
                                  onPressed: () =>
                                      Navigator.pop(context, false),
                                  child: const Text('Cancel'),
                                ),
                                ElevatedButton(
                                  onPressed: () => Navigator.pop(context, true),
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: Colors.red,
                                    foregroundColor: Colors.white,
                                  ),
                                  child: const Text('Stop'),
                                ),
                              ],
                            ),
                          );

                          if (confirm == true) {
                            await _stopService();
                          }
                        },
                        icon: const Icon(Icons.stop_circle_outlined),
                        label: const Text('Stop Protection'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.red,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 32,
                            vertical: 16,
                          ),
                        ),
                      ),

                    const SizedBox(height: 48),

                    // Info Card
                    Card(
                      color: Colors.blue.shade50,
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            Row(
                              children: [
                                Icon(
                                  Icons.info_outline,
                                  color: Colors.blue.shade700,
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Text(
                                    'Important Information',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.blue.shade700,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 12),
                            Text(
                              '• Service runs in background\n'
                              '• Works even when app is closed\n'
                              '• Auto-restarts after phone reboot\n'
                              '• Keep battery optimization OFF',
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.grey.shade700,
                                height: 1.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
