package com.example.dell.wi_fi_direct_based_videostream_ltf.Algorithmic;

import android.util.Log;

import com.example.dell.wi_fi_direct_based_videostream_ltf.wifi_direct.WiFiDirectActivity;

public class ParametersCollection implements Runnable {

//    private TrafficStats trafficStats;
    private WiFiDirectActivity wiFiDirectActivity;
    public static final String TAG = ParametersCollection.class.getSimpleName();
    private boolean isalive=true;
    public ParametersCollection(WiFiDirectActivity wiFiDirectActivity){

        this.wiFiDirectActivity=wiFiDirectActivity;
    }

    @Override
    public void run() {

//        Log.d(TAG, "run: SSID:"+wiFiDirectActivity.getSSID());

//        trafficStats=new TrafficStats();

        while(isalive){
            try {
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }
            Log.d(TAG, "run: RSSI----------:" + wiFiDirectActivity.getRSSI(wiFiDirectActivity.getSSID()));
//            Log.d(TAG, "run: Bandwidth---------:"+wiFiDirectActivity.getBandwidth(20));

            //Log.d(TAG, "run: TrafficStats--:"+TrafficStats.getTotalTxBytes());
        }

    }
    public void setSatus(boolean isalive){

        this.isalive=isalive;
    }
}
