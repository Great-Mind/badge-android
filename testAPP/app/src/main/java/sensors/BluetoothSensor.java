package sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapp.GlobalVariables;
import com.example.testapp.LoginActivity;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import interfaces.SensorFunction;
import interfaces.StateMachineRunnable;
import tools.AndroidResponse;
import tools.ClassToJson;
import tools.DataCache;
import tools.RequestSender;
import tools.SensorModuleName;

public class BluetoothSensor extends AppCompatActivity implements SensorFunction {
    private BluetoothAdapter bluetoothAdapter;
    //    private List<BluetoothDevice> devicesFound = new ArrayList<>();
//    private List<Double> distance = new ArrayList<>();//distance of nearby devices
    private long samplePeriod;
    private TextView[] distView;
    private boolean enableDisplay;
    private Thread autoSearchThread;
    private long periodCnt;
    private long scanCnt;
    private TreeMap<Double, String> nearDeviceMap;
    private HashMap<String, Double> deviceTmpMap;
    private StateMachineRunnable stateMachine;
    private String lastReallyNearMac;
    TextView tx4;
    private HashSet<String> macFilter = new HashSet<>();
    Data dataCache;

    public BluetoothSensor() {
    }

    public BluetoothSensor(BluetoothAdapter adapter, StateMachineRunnable stateMachine) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.stateMachine = stateMachine;
        samplePeriod = GlobalVariables.Parameters.BLUE_SAMPLE_PERIOD;
        enableDisplay = false;
        autoSearchThread = null;
        periodCnt = 0;
        scanCnt = 0;
        nearDeviceMap = new TreeMap<>();
        deviceTmpMap = new HashMap<>();
        lastReallyNearMac = "";
        dataCache = new Data("NearMobiles");
        for (String ss : GlobalVariables.Parameters.blueToothMacs) {
            macFilter.add(ss);
        }
        startSensor();
    }


    @Override
    public int startSensor() {
        if (autoSearchThread == null) {//use a thread to do auto search and update
            autoSearchThread = new Thread() {
                public void run() {
                    while (!isInterrupted()) {
                        try {
                            Thread.sleep(1);
                        } catch (Exception e) {
                        }

                        periodCnt++;
                        if (periodCnt >= samplePeriod) {
                            scanCnt++;
                            periodCnt = 0;
                            search();
                        }
                        if (scanCnt >= GlobalVariables.Parameters.BLUE_SCAN_TIME_LIMIT) {
                            scanCnt = 0;
                            updateDis();
                            nearDeviceMap=new TreeMap<>();
                            deviceTmpMap=new HashMap<>();
                        }
                    }
                }
            };
            autoSearchThread.start();
        }
        return 0;
    }

    @Override
    public int stopSensor() {
        autoSearchThread.interrupt();
        autoSearchThread = null;
        periodCnt = 0;
        bluetoothAdapter.disable();
        return 0;
    }

    @Override
    public int setTransferPeriod(long period) {
        return 0;
    }

    @Override
    public int setSamplePeriod(long period) {
        if (period > 100) {
            samplePeriod = period;
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int enableDisplay(TextView[] textViews) {
        enableDisplay = true;
        distView = textViews;
        tx4 = textViews[1];
        return 0;
    }

    //定义广播接收
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        //当系统接收到INTENT BROADCAST的时候， 就会被调用
        //BroadcastReceiver 是对发送出来的 Broadcast 进行过滤、接受和响应的组件。
        // 首先将要发送的消息和用于过滤的信息（Action，Category）装入一个 Intent 对象，
        // 然后通过调用 Context.sendBroadcast() 、 sendOrderBroadcast() 方法把 Intent
        // 对象以广播形式发送出去。 广播发送出去后，所以已注册的 BroadcastReceiver 会检查注册
        // 时的 IntentFilter 是否与发送的 Intent 相匹配，若匹配则会调用 BroadcastReceiver
        // 的 onReceiver() 方法

        //BluetoothAdapter的startDiscovery()，startDiscovery()返回
        // 的一个是布尔值，此值为true时候仅仅表示Android系统已经启动蓝牙
        // 模块的扫描过程，这个扫描过程是一个异步的过程。为了得到扫描结果，
        // 在一个广播接收器中异步接收Android系统发送的扫描结果

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (macFilter.contains(device.getAddress())) {
                    int rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);//获取额外rssi值
                    deviceTmpMap.put(device.getAddress(), getDistance(rssi));
                }
            }
//            else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
//            }
        }
    };

    private void search() {
        if (enableDisplay)
            tx4.setText("search");
        bluetoothAdapter.startDiscovery();
    }

    private void updateDis() {
        if (enableDisplay)
            tx4.setText("update");

        double minDis = 999.0;
        for (Map.Entry<String, Double> entry : deviceTmpMap.entrySet()) {// go through every device
            double dis = entry.getValue();
            if (dis < GlobalVariables.Parameters.BLUE_NEAR_THRESHOLD) {// near device
                nearDeviceMap.put(dis, entry.getKey());
            }
        }

        //send near devices change message to server
        ArrayList<String> macTmp = new ArrayList<>(nearDeviceMap.values());
        boolean change = false;
        if(macTmp.size()!=dataCache.a.size()){
            change=true;
        }
        if(!change){
            for(int i=0;i<macTmp.size();i++){
                if(!macTmp.get(i).equals(dataCache.a.get(i))){
                    change=true;
                    break;
                }
            }
        }
        if(change) {
            synchronized (dataCache.dataLock) {
                dataCache.clear();
                dataCache.addData(macTmp);
//                Log.d("----Bluetooth Transmit",SensorModuleName.BLUETOOTH+"-------");
                RequestSender.postDataWithParam(ClassToJson.convert(dataCache), SensorModuleName.BLUETOOTH);
                dataCache.a.remove(dataCache.a.size()-1);
            }
        }

        if (nearDeviceMap.size() > 0) {// have near devices
            Map.Entry<Double, String> entry = nearDeviceMap.firstEntry();
            minDis = entry.getKey();
            String devMac = entry.getValue();

            if (minDis < GlobalVariables.Parameters.BLUE_REALLY_NEAR_THRESHOLD) {// have really near device
                GlobalVariables.Variables.deviceCnt = nearDeviceMap.size();
                if (GlobalVariables.Variables.reallyNear == 0) {// no really near before
                    GlobalVariables.Variables.reallyNear = 2;//new really near
                    stateMachine.run();
                } else {// have really near before
                    if (!lastReallyNearMac.equals(devMac)) {//really near device changed
                        if(!GlobalVariables.Variables.isScanning) {
                            GlobalVariables.Variables.reallyNear = 2;//have changed
                            stateMachine.run();
                        }
                    }
                }
                lastReallyNearMac = devMac;
            } else {// no really near device
                if (GlobalVariables.Variables.reallyNear != 0) {// have really near before
                    GlobalVariables.Variables.reallyNear = 0;
                    if (GlobalVariables.Variables.isScanning) {// scanning is an activity , must stop camera first before running state machine
                        GlobalVariables.Variables.stopScan.stop();//stop scanning if scanning on
                        try {
                            Thread.sleep(2000);//delay for camera to stop
                        } catch (Exception e) {
                        }
                        GlobalVariables.Variables.deviceCnt = nearDeviceMap.size();
                        stateMachine.run();
                    }else{// not scanning
                        if(GlobalVariables.Variables.deviceCnt==0){
                            GlobalVariables.Variables.deviceCnt = nearDeviceMap.size();
                            stateMachine.run();
                        }
                    }
                }else{// no really near before
                    if(GlobalVariables.Variables.deviceCnt==0){
                        GlobalVariables.Variables.deviceCnt = nearDeviceMap.size();
                        stateMachine.run();
                    }
                }
            }
        }else{// no Near Device
            if(GlobalVariables.Variables.deviceCnt!=0){
                GlobalVariables.Variables.deviceCnt=0;
                GlobalVariables.Variables.reallyNear=0;

                if (GlobalVariables.Variables.isScanning) {// scanning is an activity , must stop camera first before running state machine
                    GlobalVariables.Variables.stopScan.stop();//stop scanning if scanning on
                    try {
                        Thread.sleep(2000);//delay for camera to stop
                    } catch (Exception e) {
                    }

                }
                stateMachine.run();
            }
        }
        if (enableDisplay) {
            distView[0].setText(String.format("%.2f", minDis));
        }

    }

    void nearDevicesToServer(HashMap<String, Double> deviceMap){
        String macAddr = GlobalVariables.Parameters.MY_BT_MAC_ID;
        Map<String, Object> map = new HashMap<>();
        map.put("macAddr",macAddr);
        map.put("devices",deviceMap);

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String json = ClassToJson.convert(map);
        RequestBody body;
        Request request;
        body = RequestBody.create(json, JSON);
        request = new Request.Builder().url(GlobalVariables.Parameters.LOGIN_URL).post(body).build();

        // asynchronous request with callback.
        client.newCall(request).enqueue((new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                showToast("Connection to");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response ) throws IOException {
                ResponseBody responseBody = response.body();
                Gson gson=new Gson();
                GlobalVariables.Variables.loginCode=response.code();
//                Log.d("ResponseJSon",bodyJson);
                String bodyJson = responseBody.string();
                Log.e("================ResponseJSon",bodyJson);
                if (GlobalVariables.Parameters.SERVER_LOGIN_JSON){
                    AndroidResponse androidResponse = gson.fromJson(bodyJson,AndroidResponse.class);
                    Log.i("================ResponseJSon", androidResponse.getSuccess());
                    GlobalVariables.Variables.loginResponseBody = androidResponse.getSuccess();
                }else{
                    bodyJson = bodyJson.substring(16);
                    String result = bodyJson.substring(0,bodyJson.lastIndexOf(')')).split("=")[1];
                    Log.i("================ResponseJSon",result);
                    GlobalVariables.Variables.loginResponseBody = result;
                }
                responseBody.close();
            }
        }));
    }


    public double getDistance(int rssi) {
        int iRssi = Math.abs(rssi);
        double power = (iRssi - 50) / 25.0;
        return Math.pow(10, power);
    }

    private class Data extends DataCache {
        ArrayList<String> a;

        public Data(String type) {
            super(type);
            a = new ArrayList<>();
        }

        void addData(ArrayList<String> macTmp) {
            a = macTmp;
            for (int i=0;i<macTmp.size();i++) {
                this.addTimeStamp();
            }
            a.add(GlobalVariables.Parameters.MY_BT_MAC_ID);
            this.addTimeStamp();
        }

        @Override
        public void clear() {
            // sending need synchronized, no need another here
            a = new ArrayList<>();
            time_stamp = new LinkedList<>();
        }
    }

}