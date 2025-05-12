package com.example.my_flutter_bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gg.reader.api.dal.GClient;
import com.gg.reader.api.dal.HandlerDebugLog;
import com.gg.reader.api.protocol.gx.EnumG;
import com.gg.reader.api.protocol.gx.MsgBaseGetCapabilities;
import com.gg.reader.api.protocol.gx.MsgBaseGetPower;
import com.gg.reader.api.protocol.gx.MsgBaseInventoryEpc;
import com.gg.reader.api.protocol.gx.MsgBaseSetPower;
import com.peripheral.ble.BleDevice;
import com.peripheral.ble.BleServiceCallback;
import com.peripheral.ble.BluetoothCentralManager;
import com.peripheral.ble.BluetoothCentralManagerCallback;
import com.peripheral.ble.BluetoothPeripheral;
import com.peripheral.ble.HciStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;

public class MyListener {
    private final Context applicationContext;
    private final BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
    private BasicMessageChannel<Object> messageChannel;
    private final UUID SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private final GClient client = new GClient();
    private Map<String, Object> arguments;
    private final BluetoothCentralManager central;    //  蓝牙管理类
    private Long CURRENT_ANTENNA_NUM = 0L;
    private final Map<String, Object> messageMap = new HashMap<>();
    private final Map<String, Consumer<String>> actionMap = new HashMap<>();
    private boolean APPEAR_OVER = false;

    Map<String, String> deviceMap = new HashMap<>();      // 设备名称和mac地址信息列表
    List<BluetoothPeripheral> peripherals = new LinkedList<>();   // 搜索到的设备列表
    Set<String> epcMessageSet = new HashSet<>();

    MyListener(String channelName, Context applicationContext, BinaryMessenger binaryMessenger) {

        messageChannel = new BasicMessageChannel<>(   //  实例化通信通道对象
                binaryMessenger,
                channelName,
                StandardMessageCodec.INSTANCE
        );
        this.applicationContext = applicationContext;

        Log.i("listener_channel_name", channelName);

        subscriberHandler();    // 订阅标签TCP事件
        setActionMaps();    // 初始化存放指令方法的Map

        central = new BluetoothCentralManager(    // 实例化蓝牙管理对象
                this.applicationContext,
                centralManagerCallback,
                new Handler(Looper.getMainLooper()));
        messageChannel.setMessageHandler((message, reply) -> {   // 设置通信通道对象监听方法
            arguments = castMap(message, String.class, Object.class);
            if (arguments == null) return;
            String key = getCurrentKey();
            if (key == null) {
                // key值不存在表示指令错误，不存在的指令
                //  发送信息前将集合内容清空，以免信息混乱
                Log.e("instructionInfo", "Instruction error");
                messageMap.clear();
                messageMap.put("message", "Instruction error");
                messageMap.put("isSuccessful", false);
                messageMap.put("failedCode", 10);
                messageMap.put("operationCode", 10);
                messageChannel.send(messageMap);
                return;
            }
            // 根据key值判断执行何种指令何种方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Objects.requireNonNull(actionMap.get(key)).accept(key);
            }
        });
    }

    private void setActionMaps() {
        actionMap.put("startScanner",      this :: scanBleDevice);
        actionMap.put("stopScanner",       this :: stopScanBleDevice);
        actionMap.put("connect",           this :: connectBleDevice);
        actionMap.put("closeConnect",      this :: closeBleDeviceConnect);
        actionMap.put("startReader",       this :: startReader);
        actionMap.put("startReaderEpc",    this :: startReaderEpc);
        actionMap.put("setAntennaNum",     this :: setAntennaNum);
        actionMap.put("setAntennaPower",   this :: setAntennaPower);
        actionMap.put("queryRfidCapacity", this :: queryRfidCapacity);
        actionMap.put("destroy",           this :: destroy);
    }

    private String getCurrentKey() {
        //  根据arguments（Map<String, Object>）中含有的键值设置关键字，以便判断后续需要作出怎样的操作
        String key = null;
        if (arguments.containsKey("startScanner"))            key = "startScanner";
        else if (arguments.containsKey("stopScanner"))        key = "stopScanner";
        else if (arguments.containsKey("connect"))            key = "connect";
        else if (arguments.containsKey("closeConnect"))       key = "closeConnect";
        else if (arguments.containsKey("startReader"))        key = "startReader";
        else if (arguments.containsKey("startReaderEpc"))     key = "startReaderEpc";
        else if (arguments.containsKey("setAntennaNum"))      key = "setAntennaNum";
        else if (arguments.containsKey("setAntennaPower"))    key = "setAntennaPower";
        else if (arguments.containsKey("queryRfidCapacity"))  key = "queryRfidCapacity";
        else if (arguments.containsKey("destroy"))            key = "destroy";
        return key;
    }

    private void scanBleDevice(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        peripherals.clear();
        deviceMap.clear();   // 扫描前先将相关链表清空，以免污染后续数据
        if (defaultAdapter == null) {
            unsupportedBluetooth(0);
            return;
        }
        if (!defaultAdapter.isEnabled()) {
            unopenedBluetooth(0);
            return;
        }
        if (!checkPermission()) {
            permissionDenied(0);
            return;
        }
        Log.i("ScanInfo", "Start scan");
        messageMap.clear();    //  发送信息前将集合内容清空，以免信息混乱
        messageMap.put("message", "Start scan");
        messageMap.put("isSuccessful", true);
        messageMap.put("operationCode", 1);
        messageChannel.send(messageMap);
        central.scanForPeripherals();   //  扫描附近蓝牙设备
    }
    private void stopScanBleDevice(String key) {  // 停止扫描蓝牙设备
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (defaultAdapter == null) {
            unsupportedBluetooth(1);
            return;
        }
        if (!defaultAdapter.isEnabled()) {
            unopenedBluetooth(1);
            return;
        }
        if (!checkPermission()) {
            permissionDenied(1);
            return;
        }
        Log.i("ScanInfo", "Stop scan");  //  日志，方便后续调试观察运行情况
        messageMap.clear();
        messageMap.put("message", "Stop scan");
        messageMap.put("isSuccessful", true);
        messageMap.put("operationCode", 1);
        central.stopScan();
    }
    private void connectBleDevice(String key) {
        String bluetooth_address = (String) arguments.get(key);
        if (bluetooth_address == null) return;
        if (defaultAdapter == null) {
            unsupportedBluetooth(2);
            return;
        }
        if (!defaultAdapter.isEnabled()) {
            unopenedBluetooth(2);
            return;
        }
        if (!checkPermission()) {
            permissionDenied(2);
            return;
        }
        Log.i("address", bluetooth_address);
        BluetoothPeripheral peripheral = central.getPeripheral(bluetooth_address);
        BleDevice device = setBleDevice(peripheral);
        client.openBleDevice(device);   //  连接蓝牙
    }
    @NonNull
    private BleDevice setBleDevice(BluetoothPeripheral peripheral) {
        BleDevice device = new BleDevice(central, peripheral);
        device.setServiceCallback(new BleServiceCallback() {  //  设置蓝牙服务回调函数
            @Override
            public void onServicesDiscovered(BluetoothPeripheral peripheral) {
                List<BluetoothGattService> services = peripheral.getServices();  //  获取所有的服务
                for (BluetoothGattService service : services) {
                    //示例"0000fff0-0000-1000-8000-00805f9b34fb"
                    if (service.getUuid().toString().equals(SERVICE_UUID.toString())) {
                        device.findCharacteristic(service);    // 设置为指定的服务
                    }
                }
                device.setNotify(true);
            }
        });
        return device;
    }
    private void closeBleDeviceConnect(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;   // 当值不为真时直接跳出方法，只有为真时才进行后续操作
        if (defaultAdapter == null) {
            unsupportedBluetooth(3);
            return;
        }
        if (!defaultAdapter.isEnabled()) {
            unopenedBluetooth(3);
            return;
        }
        if (!checkPermission()) {
            permissionDenied(3);
            return;
        }
        client.close();
        // 主动关闭连接
        Log.i("closeConnectInfo", "Actively close the device connection");
        messageMap.clear();
        messageMap.put("message", "The connection has been closed");
        messageMap.put("isSuccessful", true);
        messageMap.put("operationCode", 3);
        messageChannel.send(messageMap);
    }
    private void startReader(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (CURRENT_ANTENNA_NUM == 0L) {    //  当使能端口号为0时，则说明在读卡前并未设置使能端口号
            messageMap.clear();
            // 未配置天线端口，请先配置天线端口
            messageMap.put("message", "The antenna port is not configured. " +
                    "Please configure the antenna port first");
            messageMap.put("isSuccessful", false);
            messageMap.put("failedCode", 0);
            messageMap.put("operationCode", 5);
            messageChannel.send(messageMap);
            return;  //  当执行完未设置使能端口的相关操作便跳出方法，后续语句不再执行
        }
        //  实例化EPC标签读卡对象
        MsgBaseInventoryEpc msgBaseInventoryEpc = new MsgBaseInventoryEpc();
        //  设置使能端口号
        msgBaseInventoryEpc.setAntennaEnable(CURRENT_ANTENNA_NUM);
        //  设置读卡方式（轮询和单次），此处为单次
        msgBaseInventoryEpc.setInventoryMode(EnumG.InventoryMode_Single);
        client.sendSynMsg(msgBaseInventoryEpc);   //  发送读卡的同步信息
        boolean operationSuccess = false;
        //  只有当读卡对象返回代码为0时，读卡操作才是成功的
        if (0x00 == msgBaseInventoryEpc.getRtCode()) {
            // 读卡操作成功
            Log.i("readInfo", "The card reading operation was successful");
            operationSuccess = true;
            epcMessageSet.clear();
            APPEAR_OVER = false;
        } else {
            // 读卡操作失败
            Log.e("readInfo", "The card reading operation failed");
            messageMap.clear();
            messageMap.put("message", new HashMap<String, Object>() {{
                put("failedCode", msgBaseInventoryEpc.getRtCode());
                put("failedInfo", msgBaseInventoryEpc.getRtMsg());
            }});
            messageMap.put("isSuccessful", false);
            messageMap.put("failedCode", 1);
            messageMap.put("operationCode", 5);
            messageChannel.send(messageMap);
        }
        // 搞不懂为什么要在外层进行通讯才行，在里面发送的话会发送不了
        // 并且通讯方法只能在主线程中调用，无法通过创建新线程处理
        if (!operationSuccess) return;
        messageMap.clear();
        messageMap.put("message", "The card reading operation was successful");
        messageMap.put("isSuccessful", true);
        messageMap.put("operationCode", 5);
        messageChannel.send(messageMap);
    }
    private void startReaderEpc(String key) {
        /*
            读取EPC标签数据，由于标签上报的回调函数中无法进行相应通讯（似乎是阻塞了，详细原因不清楚），
            只能够添加了一个上报结束的标志,只有当上报结束标志为真时才将读取到的EPC标签数据发送给flutter端，
            否则提示flutter端EPC上报尚未结束
        */
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        Log.i("readDataInfo", "Start reading the data");
        if (APPEAR_OVER) {
            Log.i("readDataInfo", epcMessageSet.toString());
            messageMap.clear();
            messageMap.put("message", epcMessageSet);
            messageMap.put("isSuccessful", true);
        } else {
            Log.e("readDataInfo", "Unfinished reporting");
            messageMap.clear();
            messageMap.put("message", "Unfinished reporting");
            messageMap.put("failedCode", 2);
            messageMap.put("isSuccessful", false);
        }
        messageMap.put("operationCode", 6);
        messageChannel.send(messageMap);
    }
    private void setAntennaNum(String key) {   //  设置使能端口
        List<Integer> antenna_numbers = castList(arguments.get(key), Integer.class);
        if (antenna_numbers == null) return;
        List<Long> antenna_numbs = new ArrayList<>();
        StringBuilder ANTENNA_INFO = new StringBuilder();
        CURRENT_ANTENNA_NUM = 0L;
        for (Integer num : antenna_numbers) {
            antenna_numbs.add(getANTENNA_NUM(num));
        }
        for (Long num : antenna_numbs) {
            ANTENNA_INFO.append(num.toString()).append("|");
            CURRENT_ANTENNA_NUM |= num;
        }
        ANTENNA_INFO.deleteCharAt(ANTENNA_INFO.length() - 1);
        Log.i("antennaNumInfo", ANTENNA_INFO.toString());
        if (CURRENT_ANTENNA_NUM == 0L) {
            Log.e("setAntennaNumInfo", "Antenna setting failed");
            messageMap.clear();
            messageMap.put("message", "Antenna setting failed");
            messageMap.put("isSuccessful", false);
            messageMap.put("failedCode", 3);
        } else {
            Log.i("setAntennaNumInfo", "Antenna setting successful");
            messageMap.clear();
            messageMap.put("message", "Antenna setting successful");
            messageMap.put("isSuccessful", true);
        }
        messageMap.put("operationCode", 7);
        messageChannel.send(messageMap);
    }
    private Long getANTENNA_NUM(int antenna_num) {
        /*
        根据当前天线端口号（CURRENT_ANTENNA_NUM）设置读卡时使能的天线端口号（ANTENNA_NUM）
        */
        switch (antenna_num) {
            case 1:
                return EnumG.AntennaNo_1;
            case 2:
                return EnumG.AntennaNo_2;
            case 3:
                return EnumG.AntennaNo_3;
            case 4:
                return EnumG.AntennaNo_4;
            default:
                return 0L;
        }
    }
    private void setAntennaPower(String key) {   //  设置天线端口功率
        Object antennaPowers = arguments.get(key);
        if (antennaPowers == null) return;
        Map<Integer, Integer> antennaPowerMap =
                castMap(antennaPowers, Integer.class, Integer.class);
        if (antennaPowerMap == null) return;
        Log.i("antennaPowerInfo", antennaPowerMap.toString());
        Hashtable<Integer, Integer> antennaPowerTable = new Hashtable<>(antennaPowerMap);
        MsgBaseSetPower msgBaseSetPower = new MsgBaseSetPower();
        msgBaseSetPower.setDicPower(antennaPowerTable);
        client.sendSynMsg(msgBaseSetPower);
        if (msgBaseSetPower.getRtCode() == 0) {
            MsgBaseGetPower msgBaseGetPower = new MsgBaseGetPower();
            client.sendSynMsg(msgBaseGetPower);
            if (msgBaseGetPower.getRtCode() == 0) {
                Log.i("antennaPowerInfo", "The antenna power setting was successful");
                messageMap.clear();
                messageMap.put("message", msgBaseGetPower.getDicPower());
                messageMap.put("isSuccessful", true);
            } else {
                Log.e("antennaPowerInfo", "The antenna power setting failed: " +
                        "msgBaseGetPowerRtCode ==> " + msgBaseGetPower.getRtCode());
                messageMap.clear();
                messageMap.put("message", "The antenna power setting failed");
                messageMap.put("isSuccessful", false);
                messageMap.put("failedCode", 4);
            }
        } else {
            Log.e("antennaPowerInfo", "The antenna power setting failed: " +
                    "msgBaseSetPowerRtCode ==> " + msgBaseSetPower.getRtCode());
            messageMap.clear();
            messageMap.put("message", "The antenna power setting failed");
            messageMap.put("isSuccessful", false);
            messageMap.put("failedCode", 4);
        }
        messageMap.put("operationCode", 8);
        messageChannel.send(messageMap);
    }
    private void queryRfidCapacity(String key) {   // 查询蓝牙的读写能力信息
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        MsgBaseGetCapabilities msgBaseGetCapabilities = new MsgBaseGetCapabilities();
        Log.i("queryCapacityInfo", "Start the query");
        client.sendSynMsg(msgBaseGetCapabilities);
        if (msgBaseGetCapabilities.getRtCode() == 0X00) {
            messageMap.clear();
            MsgBaseGetPower msgBaseGetPower = new MsgBaseGetPower();
            client.sendSynMsg(msgBaseGetPower);
            Map<String, Integer> powerMap = getRfidMessage(msgBaseGetPower, msgBaseGetCapabilities);
            Log.i("rfidCapacityInfo", powerMap.toString());
            messageMap.put("message", powerMap);
            messageMap.put("isSuccessful", true);
        } else {
            Log.e("rfid_message", "Query failed: " +
                    "msgBaseGetCapabilitiesRtCode ==> " + msgBaseGetCapabilities.getRtCode());
            messageMap.clear();
            messageMap.put("message", "Query failed");
            messageMap.put("isSuccessful", false);
            messageMap.put("failedCode", 5);
        }
        messageMap.put("operationCode", 9);
        messageChannel.send(messageMap);
    }
    private void destroy(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        messageChannel = null;
        central.stopScan();
        client.close();
    }
    @NonNull
    private static Map<String, Integer> getRfidMessage(
            @NonNull MsgBaseGetPower msgBaseGetPower,
            MsgBaseGetCapabilities msgBaseGetCapabilities
    ) {
        Map<String, Integer> powerMap = new HashMap<>();
        if (msgBaseGetPower.getRtCode() == 0) {
            Hashtable<Integer, Integer> powers = msgBaseGetPower.getDicPower();
            for (Map.Entry<Integer, Integer> entry : powers.entrySet()) {
                powerMap.put(entry.getKey().toString(), entry.getValue());
            }
            powerMap.put("maxPower", msgBaseGetCapabilities.getMaxPower());
            powerMap.put("minPower", msgBaseGetCapabilities.getMinPower());
            powerMap.put("antennaCount", msgBaseGetCapabilities.getAntennaCount());
        }
        return  powerMap;
    }
    //  蓝牙适配器相关回调函数，这里只是重写了扫描附近蓝牙设备、以及蓝牙连接相关的方法
    BluetoothCentralManagerCallback centralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override    //  蓝牙管理类实例对象调用扫描蓝牙设备方法后会调用此方法
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            //  将蓝牙设备进行过滤，避免蓝牙设备重复、以及蓝牙设备名称为空
            if (!peripherals.contains(peripheral) && !peripheral.getName().isEmpty()) {
                Log.e("peripheralAddress", peripheral.getAddress());
                peripherals.add(peripheral);
                deviceMap.put(peripheral.getName(), peripheral.getAddress());
                messageMap.clear();
                messageMap.put("message", deviceMap);
                messageMap.put("isSuccessful", true);
                messageMap.put("operationCode", 4);
                messageChannel.send(messageMap);
            }
        }
        @Override   // 蓝牙连接成功时调用
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            Log.i("connectInfo", "Connected successful ==> " +
                    peripheral.getName() + ":" + peripheral.getAddress());
            messageMap.clear();

            messageMap.put("message", new HashMap<String, String>() {{
                put(peripheral.getName(), peripheral.getAddress());
            }});
            messageMap.put("isSuccessful", true);
            messageMap.put("operationCode", 2);
            messageChannel.send(messageMap);
        }
        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, HciStatus status) {
            Log.e("connectInfo", "Connected failed ==> " +
                    peripheral.getName() + ":" + peripheral.getAddress());
            messageMap.clear();
            messageMap.put("message", new HashMap<String, String>() {{
                put(peripheral.getName(), peripheral.getAddress());
            }});
            messageMap.put("isSuccessful", false);
            messageMap.put("failedCode", 6);
            messageMap.put("operationCode", 2);
            messageChannel.send(messageMap);
        }
        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, HciStatus status) {
            Log.i("connectInfo", "Disconnect ==> " +
                    peripheral.getName() + ":" + peripheral.getAddress());
            messageMap.clear();
            messageMap.put("message", new HashMap<String, String>() {{
                put(peripheral.getName(), peripheral.getAddress());
            }});
            messageMap.put("isSuccessful", true);
            messageMap.put("operationCode", 3);
            messageChannel.send(messageMap);
        }
    };


    private void subscriberHandler() {   //  订阅标签TCP事件
        client.onTagEpcLog = (s, logBaseEpcInfo) -> {   // EPC标签上报事件
            if (logBaseEpcInfo.getResult() == 0) {
                Log.i("epcDataInfo", logBaseEpcInfo.getEpc() + " ==> " + logBaseEpcInfo.getAntId());
                epcMessageSet.add(logBaseEpcInfo.getEpc() + "#" + logBaseEpcInfo.getAntId());
            }
        };
        client.onTagEpcOver = (s, logBaseEpcOver) -> {   //  EPC标签上报结束事件
            Log.i("handlerTagEpcOver", logBaseEpcOver.getRtMsg());
            // send();
            Log.i("epcAppearOver", epcMessageSet.toString());
            APPEAR_OVER = true;
        };

        client.debugLog = new HandlerDebugLog() {   // 错误日志
            public void sendDebugLog(String msg) {
                Log.e("sendDebugLog",msg);
            }

            public void receiveDebugLog(String msg) {
                Log.e("receiveDebugLog",msg);
            }
        };
    }

    private void unsupportedBluetooth(int code) {
        messageMap.clear();
        messageMap.put("message", "This device does not support Bluetooth");
        messageMap.put("isSuccessful", false);
        messageMap.put("failedCode", 7);
        messageMap.put("operationCode", code);
        messageChannel.send(messageMap);
    }
    private void unopenedBluetooth(int code) {
        messageMap.clear();
        messageMap.put("message", "Bluetooth is not turned on. Please turn it on");
        messageMap.put("isSuccessful", false);
        messageMap.put("failedCode", 8);
        messageMap.put("operationCode", code);
        messageChannel.send(messageMap);
    }
    private void permissionDenied(int code) {
        // 权限不足，请检查相关权限并授予
        messageMap.clear();
        messageMap.put("message", "Insufficient permissions. " +
                "Please check the relevant permissions and grant them");
        messageMap.put("isSuccessful", false);
        messageMap.put("failedCode", 9);
        messageMap.put("operationCode", code);
        messageChannel.send(messageMap);
    }
    private boolean checkPermission() {
        int denied = PackageManager.PERMISSION_DENIED;
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.BLUETOOTH) == denied) {
            Log.e("notPermission", "BLUETOOTH");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.BLUETOOTH_SCAN) == denied) {
                Log.e("notPermission", "BLUETOOTH_SCAN");
                return false;
            }
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.BLUETOOTH_ADMIN) == denied) {
            Log.e("notPermission", "BLUETOOTH_ADMIN");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.BLUETOOTH_CONNECT) == denied) {
                Log.e("notPermission", "BLUETOOTH_CONNECT");
                return false;
            }
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == denied) {
            Log.e("notPermission", "ACCESS_FINE_LOCATION");
            return false;
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == denied) {
            Log.e("notPermission", "ACCESS_COARSE_LOCATION");
            return false;
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == denied) {
            Log.e("notPermission", "ACCESS_BACKGROUND_LOCATION");
            return false;
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) == denied) {
            Log.e("notPermission", "READ_EXTERNAL_STORAGE");
            return false;
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == denied) {
            Log.e("notPermission", "WRITE_EXTERNAL_STORAGE");
            return false;
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.INTERNET) == denied) {
            Log.e("notPermission", "INTERNET");
            return false;
        }
        return true;
    }

    private <V> List<V> castList(Object obj, Class<V> value) {
        /*
        对对象转换为List类型作出检查
         */
        List<V> list = new ArrayList<>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>)obj) {
                list.add(value.cast(o));
            }
            return list;
        }
        return null;
    }
    private <K, V> Map<K, V> castMap(Object obj, Class<K> key, Class<V> value) {
        /*
        对于对象转换为Map类型作出检查
        */
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
