
import 'package:flutter/services.dart';

import 'my_flutter_bluetooth_platform_interface.dart';

class MyFlutterBluetooth {
  Future<String?> getPlatformVersion() {
    return MyFlutterBluetoothPlatform.instance.getPlatformVersion();
  }
  static const MethodChannel _channel =
  MethodChannel('my_flutter_bluetooth');

  static Future<String?> Init() async {
    final String? code = await _channel.invokeMethod('init');
    return code;
  }
}
