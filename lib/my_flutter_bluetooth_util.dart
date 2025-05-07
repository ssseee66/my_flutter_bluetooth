import 'dart:async';

import 'package:flutter/services.dart';
import 'package:my_flutter_bluetooth/bluetooth_operation_code.dart';

class MyFlutterBluetoothUtil {
  MyFlutterBluetoothUtil._();

  factory MyFlutterBluetoothUtil() => _instance;
  static final MyFlutterBluetoothUtil _instance = MyFlutterBluetoothUtil._();

  BasicMessageChannel flutterChannel = const BasicMessageChannel("my_flutter_bluetooth", StandardMessageCodec());
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
  void destroy() {
    messageChannel.send({"destroy": true});
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
}