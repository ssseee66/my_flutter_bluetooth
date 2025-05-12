package com.example.my_flutter_bluetooth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.StandardMessageCodec;

/** RfidReaderPlugin */
public class MyFlutterBluetoothPlugin implements FlutterPlugin {
    private static final String FLUTTER_TO_ANDROID_CHANNEL = "my_flutter_bluetooth";
    private Context applicationContext;
    private MyListener listener;
    private BasicMessageChannel<Object> flutter_channel;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.e("onAttachedToEngine", "onAttachedToEngine");
        applicationContext = flutterPluginBinding.getApplicationContext();

        flutter_channel = new BasicMessageChannel<>(
            flutterPluginBinding.getBinaryMessenger(),
            FLUTTER_TO_ANDROID_CHANNEL,
            StandardMessageCodec.INSTANCE
        );

        flutter_channel.setMessageHandler((message, reply) -> {
            Map<String, Object> channelMessage = castMap(message, String.class, Object.class);
            if (channelMessage == null) return;
            if (channelMessage.containsKey("channelName")) {
                String channel_name = (String) channelMessage.get("channelName");
                if (channel_name == null) return;
                Log.e("channelName", channel_name);
                listener = new MyListener(
                    channel_name,
                    applicationContext,
                    flutterPluginBinding.getBinaryMessenger()
                );
          }
        });
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
//        listener.setMessage_channel(null);
        listener = null;
        flutter_channel = null;
    }
    public static <K, V> Map<K, V> castMap(Object obj, Class<K> key, Class<V> value) {
        Map<K, V> map = new HashMap<>();
        if (obj instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                map.put(key.cast(entry.getKey()), value.cast(entry.getValue()));
            }
            return map;
        }
        return null;
    }
}
