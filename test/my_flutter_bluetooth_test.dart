import 'package:flutter_test/flutter_test.dart';
import 'package:my_flutter_bluetooth/my_flutter_bluetooth.dart';
import 'package:my_flutter_bluetooth/my_flutter_bluetooth_platform_interface.dart';
import 'package:my_flutter_bluetooth/my_flutter_bluetooth_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMyFlutterBluetoothPlatform
    with MockPlatformInterfaceMixin
    implements MyFlutterBluetoothPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MyFlutterBluetoothPlatform initialPlatform = MyFlutterBluetoothPlatform.instance;

  test('$MethodChannelMyFlutterBluetooth is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMyFlutterBluetooth>());
  });

  test('getPlatformVersion', () async {
    MyFlutterBluetooth myFlutterBluetoothPlugin = MyFlutterBluetooth();
    MockMyFlutterBluetoothPlatform fakePlatform = MockMyFlutterBluetoothPlatform();
    MyFlutterBluetoothPlatform.instance = fakePlatform;

    expect(await myFlutterBluetoothPlugin.getPlatformVersion(), '42');
  });
}
