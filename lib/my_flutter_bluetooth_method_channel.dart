import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'my_flutter_bluetooth_platform_interface.dart';

/// An implementation of [MyFlutterBluetoothPlatform] that uses method channels.
class MethodChannelMyFlutterBluetooth extends MyFlutterBluetoothPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('my_flutter_bluetooth');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
