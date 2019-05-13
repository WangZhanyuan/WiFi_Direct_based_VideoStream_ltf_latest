package com.example.dell.wi_fi_direct_based_videostream_ltf.Cache;

import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoClient;

import android.os.CountDownTimer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCache {
    private static final String TAG = "ServerCache";
    private ConcurrentHashMap<Integer, ByteBuffer> sCache = new ConcurrentHashMap<>();
    private int cacheSize;//ServerCache中存放的数据包个数，根据Log来看每秒大概15个包左右
    private long cycleTime;//删除过期数据的周期,单位ms
    private EchoClient client = new EchoClient("192.168.49.234");


    //倒计时类，间隔countDownInterval调用onTick方法，计时cycleTime/countDownInterval秒后调用onFinish方法
    private CountDownTimer countDownTimer ;

    //构造方法
    public ServerCache (int cacheSize, long cycleTime) {
        setCacheSize(cacheSize);
        setCycleTime(cycleTime);
        Log.d(TAG, "ServerCache: ServerCache is starting");
        countDownTimer= new CountDownTimer(cycleTime,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG,"onTick");
            }

            @Override
            public void onFinish() {
                //倒计时结束的操作
            deleteStaleData();
            this.start();
            }
        }.start();
        Log.d(TAG,"CountDownTimer Start");
    }

    public void setCacheSize (int size) { this.cacheSize = size; }
    public void setCycleTime (long time) { this.cycleTime = time; }

    //每一次put，先发送过去，再放到sCache备份，以便丢包后请求
    public void put (int stamp, ByteBuffer buffer) {
        sendByEchoClient(stamp, buffer);
        sCache.put(stamp, buffer);
        Log.d(TAG,"sCache size : "+sCache.size());
    }

    //请求发送对应时间戳的数据
    public void get (int stamp) {
        sendByEchoClient(stamp, sCache.get(stamp));
    }

    public void deleteStaleData () {
        //遍历找到最大的key
        int maxStamp = 0;
        for (int tempStamp : sCache.keySet()) {
                maxStamp = tempStamp > maxStamp ? tempStamp : maxStamp;
        }
        Log.d(TAG, "deleteStaleData: stamp "+maxStamp);
        Log.d(TAG, "deleteStaleData: deleteStaleData is running");
        //遍历sCache，删除间隔大于cacheSize的元素
        Iterator<Map.Entry<Integer, ByteBuffer>> iter = sCache.entrySet().iterator();
        Log.d(TAG, "deleteStaleData: Iterator init");
        while (iter.hasNext()) {
            Log.d(TAG, "deleteStaleData: iter.hasnext");
            Map.Entry<Integer, ByteBuffer> entry =  iter.next();
            Log.d(TAG, "deleteStaleData: iter.next");
            Log.d(TAG, "deleteStaleData: entry.getKey"+entry.getKey());
            Log.d(TAG, "deleteStaleData: maxStamp"+maxStamp);
            Log.d(TAG, "deleteStaleData: cacheSize"+cacheSize);
            if (maxStamp - entry.getKey() > cacheSize) {
                Log.d(TAG, "deleteStaleData: >cachesize");
                iter.remove();
                Log.d(TAG, "deleteStaleData: data is deleted");
            }
        }
    }

    //发送数据
    public void sendByEchoClient (int stamp, ByteBuffer buffer) {
        if (buffer != null) {
            byte[] data = generateUDPMessage(stamp, decodeValue(buffer));
            try {
                client.sendStream_n(data, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //ByteBuffer转Byte[]
    public byte[] decodeValue(ByteBuffer buffer) {
        int len = buffer.limit() - buffer.position();
        byte[] data = new byte[len];
        buffer.get(data);
        return data;
    }

    //将时间戳与编码结果打包，相应地，需要接收端解包
    public byte[] generateUDPMessage (int stamp, byte[] data) {
        byte[] message = new byte[4 + data.length];
        System.arraycopy(int2ByteArray(stamp), 0, message, 0, 4);
        System.arraycopy(data, 0, message, 4, data.length);
        return message;
    }

    public static byte[] int2ByteArray(int i){
        byte[] result=new byte[4];
        result[0]=(byte)((i >> 24)& 0xFF);
        result[1]=(byte)((i >> 16)& 0xFF);
        result[2]=(byte)((i >> 8)& 0xFF);
        result[3]=(byte)(i & 0xFF);
        return result;
    }
}
