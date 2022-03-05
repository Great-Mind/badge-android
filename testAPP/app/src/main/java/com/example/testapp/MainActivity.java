package com.example.testapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapp.recorder.VoiceRecorder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.qrcode.encoder.QRCode;
import com.konovalov.vad.VadConfig;

import interfaces.StateMachineRunnable;
import sensors.AccelerometerSensor;
import sensors.BluetoothSensor;
import sensors.MicrophoneSensor;
import sensors.ProximitySensor;
import tools.RequestSender;
import tools.SensorModuleName;

public class MainActivity extends AppCompatActivity {
    TextView statemachineTx;
    Button bt1;
    Button btCam;
    Button resetBt;
    ImageView blackScreen;
    TextView micState;
    TextView badge2Id;
    AccelerometerSensor accSensor;
    ProximitySensor proxSensor;
    BluetoothSensor bluetoothSensor;
    MicrophoneSensor microphoneSensor;
    Object stateMachineLock;
    final int IDLE = (0);
    final int RECORDING = (1);
    final int SCANNING = (2);
    int state;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//force portrait


        statemachineTx = findViewById(R.id.smTx);
        GlobalVariables.Variables.deLog=findViewById(R.id.debug);

        // all initialized
        sensorInit();

        //  three buttons
        bt1 = findViewById(R.id.bt1);
        btCam = findViewById(R.id.btCam);
        resetBt = findViewById(R.id.resetBt);

        // set a black image as background
        blackScreen=findViewById(R.id.blackScreen);


        if(GlobalVariables.Parameters.SHOW_NOTHING){
            bt1.setVisibility(View.GONE);
            btCam.setVisibility(View.GONE);
            resetBt.setVisibility(View.GONE);
            blackScreen.setVisibility(View.VISIBLE);
        }else{
            // for test
            blackScreen.setVisibility(View.GONE);
            bt1.setText("stop");
            bt1.setOnClickListener(new ButtonClick());

            // press btCam
            btCam.setOnClickListener((x) -> {
                GlobalVariables.Variables.reallyNear=2;
                GlobalVariables.Variables.deviceCnt = 1;
                // while btCam pressed
                runStateMachine();
                return;
            });

            // press resetBt for test
            resetBt.setOnClickListener((x) -> {
                stateChangeToIdle();
            });
        }


        // initialize the stateMachineLock
        stateMachineLock = new Object();
        state = IDLE;
        micState = findViewById(R.id.micState);

        // first change to Idle since when initialized there is should not begin record first
        stateChangeToIdle();

        badge2Id = findViewById(R.id.badge2id);

        //////////////////////////////for Voice detection test
        if(GlobalVariables.Parameters.VOICE_DETECT) {
            micState.setText("On");
            if (GlobalVariables.Parameters.START_MIC) {
                microphoneSensor.startSensor();
            }
            state = RECORDING;
            statemachineTx.setText("Voice Detect Test");
        }
        /////////////////////////////////for Voice detection test

        keepScreenOn();
    }

    private void sensorInit() {
        GlobalVariables.Variables.stateMachine = new RunStateMachine();

        TextView[] tx;//3 acc sensor
        SensorManager sensorManager;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        tx = new TextView[3];
        tx[0] = findViewById(R.id.tx1);
//        tx[1] = findViewById(R.id.debug);
//        tx[2] = findViewById(R.id.tx3);

        //initiate accelerometer
        if(GlobalVariables.Parameters.START_ACC) {
            accSensor = new AccelerometerSensor(sensorManager, new RunStateMachine());
            accSensor.enableDisplay(tx);
        }

        //initiate proximity sensor
        if(GlobalVariables.Parameters.START_PROXI) {
            proxSensor = new ProximitySensor(sensorManager, new RunStateMachine());
            proxSensor.enableDisplay(tx);
        }

        //initiate bluetooth
        if(GlobalVariables.Parameters.START_BLUE) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothSensor = new BluetoothSensor(bluetoothAdapter, new RunStateMachine());
            startBluetooth(bluetoothAdapter, bluetoothSensor.mReceiver);
            bluetoothSensor.enableDisplay(new TextView[]{findViewById(R.id.blueData), findViewById(R.id.textView4)});
        }

        //initiate Microphone
        if(GlobalVariables.Parameters.START_MIC) {
            microphoneSensor = new MicrophoneSensor(false);//not write to local file
            microphoneSensor.enableDisplay(new TextView[]{findViewById(R.id.micData)});
        }

        //initiate Scanning
    }

    class ButtonClick implements View.OnClickListener {
        public ButtonClick() {

        }

        @Override
        public void onClick(View view) {
            if (bt1.getText().toString().equals("start")) {
                accSensor.startSensor();
                bt1.setText("stop");
            } else {
                bt1.setText("start");
                accSensor.stopSensor();
            }
        }
    }

    private class RunStateMachine implements StateMachineRunnable {
        public RunStateMachine() {
        }

        @Override
        public boolean run() {
            runStateMachine();
            return true;
        }
    }

    private void runStateMachine() {
        // an object to hold the lock
        synchronized (stateMachineLock) {
            switch (state) {
                case IDLE:
                    if(GlobalVariables.Variables.reallyNear > 0){
                        // really near device detected
                        GlobalVariables.Variables.reallyNear = 1;
                        Log.i("====","Badge detected!");
                        showToast("badge detected!!");
                        stateChangeToRecording();
                    }else if (GlobalVariables.Variables.deviceCnt > 0) {
                        //  near device discovered
                        stateChangeToRecording();
                    }else{
                        stateChangeToIdle();
                    }
                    break;
                case RECORDING:
                    if (GlobalVariables.Variables.reallyNear == 2) {
                        // really near device changed
                        GlobalVariables.Variables.reallyNear = 1;
                        Log.i("====","Badge detected!");
                        showToast("badge detected!!");
                        stateChangeToRecording();
                    }
                    if (GlobalVariables.Variables.deviceCnt <= 0) {
                        // no near device detected, detach
                        showToast("no near badge detected");
                        stateChangeToIdle();
                    }else{
                        stateChangeToIdle();
                    }
                    break;
//                case SCANNING:
//                    if (GlobalVariables.Variables.haveQR) {
//                        //  QR code discovered
//                        GlobalVariables.Variables.haveQR = false;
//                        sendQRCode();
//                        stateChangeToRecording();
//                    } else if (GlobalVariables.Variables.reallyNear == 0) {
//                        // no really near device detected
//                        if(GlobalVariables.Variables.deviceCnt>0) {
//                            stateChangeToRecording();
//                        }else{
//                            stateChangeToIdle();
//                        }
//                    }else{
//                        GlobalVariables.Variables.reallyNear = 1;
//                        stateChangeToRecording();
//                    }
//                    break;
                default:
                    stateChangeToIdle();
                    break;
            }
        }
    }

    private void sendQRCode(){
        RequestSender.postDataWithParam(GlobalVariables.Variables.qrCode, SensorModuleName.QRCODE);
    }

//    private void stateChangeToScanning() {
//        state = SCANNING;
//        statemachineTx.setText("Scanning");
//
//        if(GlobalVariables.Parameters.START_MIC) {
//            microphoneSensor.startSensor();
//        }
//        //start camera
//        startScanning();
//    }

    private void stateChangeToRecording() {
        //Start microphone
        micState.setText("On");
        if(GlobalVariables.Parameters.START_MIC) {
            microphoneSensor.startSensor();
        }
        state = RECORDING;
        statemachineTx.setText("Recording");
    }

    private void stateChangeToIdle() {
        //stop microphone
        micState.setText("Off");
        if(GlobalVariables.Parameters.START_MIC) {
            microphoneSensor.stopSensor();
        }

        state = IDLE;
        statemachineTx.setText("Idle");
    }

/** the following functions are not for main state machine, but needed to be done in mainActivity**/
/**********************************************************************************************/
    /**********************************************************************************************/
    private void startBluetooth(BluetoothAdapter bluetoothAdapter, BroadcastReceiver mReceiver) {
        final int REQUEST_ENABLE_BT = (1);

        //注册设备被发现时的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        //注册一个搜索结束时的广播
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter2);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void startScanning() {
        // jump to scan
        Intent scanAct = new Intent(getApplicationContext(), SimpleScannerActivity.class);
        startActivity(scanAct);
    }

    private void keepScreenOn(){

        if(GlobalVariables.Parameters.SCREEN_ON) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showToast(String str){
        Context thisContext = this;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(thisContext, str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}




