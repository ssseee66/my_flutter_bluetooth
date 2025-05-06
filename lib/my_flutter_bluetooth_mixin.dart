import 'dart:async';

import 'package:flutter/material.dart';
import 'package:my_flutter_bluetooth/my_flutter_bluetooth_util.dart';

mixin MyFlutterBluetoothMixin<T extends StatefulWidget> on State<T> {
  late StreamSubscription streamSubscription;
  final MyFlutterBluetoothUtil util = MyFlutterBluetoothUtil();

  @override
  void initState() {
    super.initState();
    util.flutterChannel.setMessageHandler(setMessageChannelHandle);
    util.setMessageChannel(hashCode.toString(), listenerBluetoothAndroidHandle);
    util.sendChannelName("channelName", hashCode.toString());
  }
  @override
  void dispose() {
    // TODO: implement dispose
    super.dispose();
    util.flutterChannel.setMessageHandler(null);
    util.messageChannel.setMessageHandler(null);
  }

  Future<void> listenerBluetoothAndroidHandle(dynamic message);
  Future<void> setMessageChannelHandle(dynamic message);

}