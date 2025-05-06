import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'my_flutter_bluetooth_method_channel.dart';

abstract class MyFlutterBluetoothPlatform extends PlatformInterface {
  /// Constructs a MyFlutterBluetoothPlatform.
  MyFlutterBluetoothPlatform() : super(token: _token);

  static final Object _token = Object();

  static MyFlutterBluetoothPlatform _instance = MethodChannelMyFlutterBluetooth();

  /// The default instance of [MyFlutterBluetoothPlatform] to use.
  ///
  /// Defaults to [MethodChannelMyFlutterBluetooth].
  static MyFlutterBluetoothPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MyFlutterBluetoothPlatform] when
  /// they register themselves.
  static set instance(MyFlutterBluetoothPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
