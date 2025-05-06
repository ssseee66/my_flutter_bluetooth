import 'dart:async';

import 'package:flutter/services.dart';
import 'package:my_flutter_bluetooth/bluetooth_failed_code.dart';
import 'package:my_flutter_bluetooth/bluetooth_operation_code.dart';

class MyFlutterBluetoothUtil {
  MyFlutterBluetoothUtil._();

  factory MyFlutterBluetoothUtil() => _instance;
  static final MyFlutterBluetoothUtil _instance = MyFlutterBluetoothUtil._();

  String messageChannelName = "";
  BasicMessageChannel flutterChannel = const BasicMessageChannel("flutter_bluetooth_android", StandardMessageCodec());
  BasicMessageChannel messageChannel = const BasicMessageChannel("null", StandardMessageCodec());

  void sendMessageToAndroid(String methodName, dynamic arg) async {
    messageChannel.send({methodName: arg});
  }

  void sendChannelName(String methodName, dynamic channelName) async {
    flutterChannel.send({methodName: channelName});
  }

  void setMessageChannel(String channelName, Future<dynamic> Function(dynamic message) handler) {
    messageChannel = BasicMessageChannel(channelName, const StandardMessageCodec());
    messageChannel.setMessageHandler(handler);
  }
  void startScan() {
    messageChannel.send({"startScanner": true});
  }
  void stopScan() {
    messageChannel.send({"stopScanner": true});
  }
  void connect(String address) {
    messageChannel.send({"connect": address});
  }
  void closeConnect() {
    messageChannel.send({"closeConnect": true});
  }
  void setAntennaNum(List<int> antennaNumList) {
    messageChannel.send({"setAntennaNum": antennaNumList});
  }
  void reader() {
    messageChannel.send({"startReader": true});
  }
  void readerData() {
    messageChannel.send({"startReaderEpc": true});
  }
  Enum getOperationAction(int code) {
    switch (code) {
      case 0:
        return BluetoothOperationCode.START_SCAN;
      case 1:
        return BluetoothOperationCode.STOP_SCAN;
      case 2:
        return BluetoothOperationCode.CONNECT;
      case 3:
        return BluetoothOperationCode.CLOSE_CONNECT;
      case 4:
        return BluetoothOperationCode.BLUETOOTH_LIST;
      case 5:
        return BluetoothOperationCode.READER;
      case 6:
        return BluetoothOperationCode.READER_DATA;
      case 7:
        return BluetoothOperationCode.SET_ANTENNA;
      case 8:
        return BluetoothOperationCode.SET_ANTENNA_POWER;
      case 9:
        return BluetoothOperationCode.QUERY_RFID_CAPACITY;
      case 10:
        return BluetoothOperationCode.INSTRUCTION_ERROR;
      default:
        return BluetoothOperationCode.OTHER_ERROR;
    }
  }
  Enum getFailedCode(int code) {
    switch (code) {
      case 0:
        return BluetoothFailedCode.NOT_SET_ANTENNA_NUM;
      case 1:
        return BluetoothFailedCode.READER_OPERATION_FAILED;
      case 2:
        return BluetoothFailedCode.NOT_APPEAR_OVER;
      case 3:
        return BluetoothFailedCode.SET_ANTENNA_NUM_FAILED;
      case 4:
        return BluetoothFailedCode.SET_ANTENNA_POWER_FAILED;
      case 5:
        return BluetoothFailedCode.QUERY_RFID_CAPACITY_FAILED;
      case 6:
        return BluetoothFailedCode.CONNECT_FAILED;
      case 7:
        return BluetoothFailedCode.NOT_SUPPORT_BLUETOOTH;
      case 8:
        return BluetoothFailedCode.BLUETOOTH_NOT_TURN_ON;
      case 9:
        return BluetoothFailedCode.PERMISSION_DENIED;
      default:
        return BluetoothFailedCode.ERROR_CODE;
    }
  }
}