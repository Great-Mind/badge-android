package com.example.testapp;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.testapp.recorder.VoiceRecorder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.konovalov.vad.VadConfig;

import java.util.HashMap;

import interfaces.StateMachineRunnable;
import interfaces.StopScan;

public class GlobalVariables {
    private GlobalVariables() {
    }

    public static class Variables {
        public static int deviceCnt = 0;
        public static boolean haveQR = false;
        public static String qrCode = "";
        public static StopScan stopScan = null;
        public static boolean isScanning = false;
        public static StateMachineRunnable stateMachine = null;
        public static int reallyNear = 0;// 0 no really near, 1 have really near, 2 really near changed
        public static TextView deLog = null;
        public static int loginCode = 0;// code from login server response
        public static String loginResponseBody = "";
    }

    public static class Parameters {
        //badge basic info
        public static final String badgeId = "device-N4-06";
        public static String dataSetId = "Nokia4";
//        public static final String SERVER_URL = "https://6zowfrzywc.execute-api.us-west-2.amazonaws.com/dev/api/";
        // public static final String SERVER_URL="http://192.168.0.4:8080/badge/";
//        public static final String SERVER="http://34.238.246.224:8080";
        // cornell server IP address
        public static final String SERVER="http://128.253.128.12:8080";
        public static final String SERVER_URL=SERVER+"/dev/api";
        public static final String LOGIN_URL=SERVER+"/dev/login";

        // it needs to be customized for every machine
        // N3-11
//         public  static  final String MY_BT_MAC_ID="04:F1:28:07:07:0B";
        // N1-12
//        public  static  final String MY_BT_MAC_ID="A8:3E:0E:B7:5B:AC";
        // N1-13
//        public  static  final String MY_BT_MAC_ID="A8:3E:0E:BB:41:11";
        // N1-14
       // public  static  final String MY_BT_MAC_ID="A8:3E:0E:B7:5C:F4";
        //N1-15
//      public static final String MY_BT_MAC_ID="A8:3E:0E:B7:59:AA";

        // N4-01
      //  public  static  final String MY_BT_MAC_ID="74:8A:28:80:EE:6C";
        //  N4-02
      //  public  static  final String MY_BT_MAC_ID="74:8A:28:80:ED:2E";
        // N4-03
//        public static final String MY_BT_MAC_ID="74:8A:28:80:ED:BD";
        // N4-04
//        public static final String MY_BT_MAC_ID="74:8A:28:80:EB:50";
        // N4-05
//        public static final String MY_BT_MAC_ID="74:8A:28:80:EB:70";
        // N4-06
        public static final String MY_BT_MAC_ID="74:8A:28:80:EE:AE";
        //global settings
        public static final boolean ALLOW_TRANSFER=true;
        public static final boolean START_BLUE=true;
        public static final boolean START_ACC=true;
        public static final boolean ACC_FIX=true;
        public static final boolean START_MIC=true;
        public static final boolean VOICE_DETECT=true;
        public static final boolean VOICE_ACTIVITY_DETECT=true;
        public static final boolean VOICE_ALWAYS_SEND=true;
        public static final boolean SCREEN_ON = true;
        public static final boolean SHOW_NOTHING=false;

        public static final boolean LOGIN =true;
        public static final boolean SERVER_LOGIN=true;
        public static final boolean SERVER_LOGIN_JSON=true;

        public static final boolean START_PROXI=true;


        //Bluetooth
        public static final long BLUE_SAMPLE_PERIOD = 3000;// bluetooth scan period (ms)
        public static final long BLUE_SCAN_TIME_LIMIT = 5;//scan times for one output
        public static final double BLUE_NEAR_THRESHOLD = 10.0;//threshold for near device (meter)
        public static final double BLUE_REALLY_NEAR_THRESHOLD = 2;//threshold for really near device (meter)
        public static String[] blueToothMacs = new String[]{
                "2C:41:A1:F5:B9:F6","5A:BF:46:B0:A9:CE",
                "A8:3E:0E:B7:74:F6","A8:3E:0E:B7:7C:34",
                "70:3C:69:48:0E:B0","A8:3E:0E:B7:78:EA",
                "48:01:C5:0C:31:93","A8:3E:0E:B7:7A:EC",
                "A8:3E:0E:BB:60:51","AC:57:75:01:61:AB",
                "04:F1:28:07:07:A7","04:F1:28:07:DD:22",
                "74:42:8B:27:12:21","04:F1:28:08:7C:0B",
                "04:F1:28:08:7C:A7","74:8A:28:80:F4:0C",
                "74:8A:28:80:F2:CE","74:8A:28:80:F3:5D",
                "74:8A:28:80:F0:F0","74:8A:28:80:F1:10",
                "74:8A:28:80:F2:4E"};

        // Bluetooth Mac map key->bluetooth mac address; value->wifi mac address
        public static HashMap<String,String> blue2MacMap = new HashMap<String,String>(){
            {
                put("A8:3E:0E:B7:7A:EC","A8:3E:0E:B7:5B:AC");//N1-12
                put("A8:3E:0E:BB:60:51","A8:3E:0E:BB:41:11");//N1-13
                put("A8:3E:0E:B7:5C:F4","A8:3E:0E:B7:7C:34");//N1-14
                put("A8:3E:0E:B7:78:EA","A8:3E:0E:B7:59:AA");//N1-15
                put("04:F1:28:08:7C:A7","04:F1:28:07:07:0B");//N3-11
                put("74:8A:28:80:F4:0C","74:8A:28:80:EE:6C");//N4-01
                put("74:8A:28:80:F2:CE","74:8A:28:80:ED:2E");//N4-02
                put("74:8A:28:80:F3:5D","74:8A:28:80:ED:BD");//N4-03
                put("74:8A:28:80:F0:F0","74:8A:28:80:EB:50");//N4-04
                put("74:8A:28:80:F1:10","74:8A:28:80:EB:70");//N4-05
                put("74:8A:28:80:F2:4E","74:8A:28:80:EE:AE");//N4-06
            }
        };
        // Bluetooth Mac map key->bluetooth mac address; value->wifi mac address
        public static HashMap<String,String> blue2DeviceMap = new HashMap<String,String>(){
            {
                put("A8:3E:0E:B7:7A:EC","N1-12");//N1-12
                put("A8:3E:0E:BB:60:51","N1-13");//N1-13
                put("A8:3E:0E:B7:5C:F4","N1-14");//N1-14
                put("A8:3E:0E:B7:78:EA","N1-15");//N1-15
                put("04:F1:28:08:7C:A7","N3-11");//N3-11
                put("74:8A:28:80:F4:0C","N4-01");//N4-01
                put("74:8A:28:80:F2:CE","N4-02");//N4-02
                put("74:8A:28:80:F3:5D","N4-03");//N4-03
                put("74:8A:28:80:F0:F0","N4-04");//N4-04
                put("74:8A:28:80:F1:10","N4-05");//N4-05
                put("74:8A:28:80:F2:4E","N4-06");//N4-06
            }
        };

        //accelerometer
        public static final long ACC_SAMPLE_DIV = 2;// sample rate about 60,000 us=0.06s     need: 8/s
        public static final long ACC_TRANSFER_PERIOD = 6000;//acc data transfer period ms    need: 1 per 6s
        public static final long ACC_FIX_PERIOD = 2000;//acc data fix accumulation period ms

        //microphone
        public static final long MIC_SAMPLE_DIV = 5;// sample rate about  ???
        public static final long MIC_TRANSFER_PERIOD = 6000;//mic data transfer period ms


        public static final boolean ACC_NETWORK_TEST = false;
    }


    public static class Encryption{
        public static final boolean encryption = true;
        public static final String secretKey="This is a random encryption key";
        public static final String algorithm = "SHA-256"; // "MD5"
    }


}
