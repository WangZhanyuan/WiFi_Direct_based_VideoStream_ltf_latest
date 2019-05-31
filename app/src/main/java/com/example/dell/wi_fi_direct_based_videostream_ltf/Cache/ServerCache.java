package com.example.dell.wi_fi_direct_based_videostream_ltf.Cache;

import android.os.CountDownTimer;
import android.util.Log;

import com.example.dell.wi_fi_direct_based_videostream_ltf.Coder.RateAdaptiveEncoder;
import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCache {
    private static final String TAG = "ServerCache";
    private int sCacheSize;//ServerCache中存放的数据包个数，根据Log来看每秒大概15个包左右
    private int timeStamp;//时间戳，存在溢出风险，以后再写怎么处理
    private int mBitrate;//测试用，仅用来测试从哪个缓冲区发送的数据
    private boolean isActive;
    private boolean needSPS_PPS;
    private byte[] sps_pps;
    private RateAdaptiveEncoder rateAdaptiveEncoder;
    private ConcurrentHashMap<Integer, byte[]> sCache = new ConcurrentHashMap<>();//神奇嗷，ConcurrentHashMap就好用了嗷
    private EchoClient client = new EchoClient("192.168.49.1");//49.31是HM作为发送端，49.234是P20作为发送端


    //倒计时类，间隔countDownInterval调用onTick方法，计时cycleTime/countDownInterval秒后调用onFinish方法
    private CountDownTimer countDownTimer ;

    //构造方法
    public ServerCache(int sCacheSize, long cycleTime, int mBitrate, RateAdaptiveEncoder rateAdaptiveEncoder) {
        this.sCacheSize = sCacheSize;
        countDownTimer= new CountDownTimer(cycleTime,1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                //倒计时结束的操作
                deleteStaleData();
                this.start();
            }
        }.start();
        this.mBitrate = mBitrate;
        this.needSPS_PPS = false;
        this.rateAdaptiveEncoder = rateAdaptiveEncoder;
    }

    public void put (byte[] data) {
//        Log.d(TAG, "put: my bitrate : "+mBitrate+" I wanna see this frame : " + Arrays.toString(data));
//        if (sps_pps == null) {
//            Log.d(TAG, "put: I will record SPS_PPS");
////            if (!getSPS_PPS_SEI(data)) {
////                sps_pps_sei = getSPS_PPS_SEIFromDefaultCache();
////            }
//            sps_pps = new byte[data.length];
//            System.arraycopy(data,0,sps_pps,0,data.length);
//            sCache.put(timeStamp++, sps_pps);
//            Log.d(TAG, "put: sps message : "+Arrays.toString(sps_pps));
//            return;
//        }
//        if (needSPS_PPS) {
////            if (!isIDRFrame(data)) {
////                return;
////            } else {
//                needSPS_PPS = false;
//                sCache.put(timeStamp++, data);
////            }
//        } else {
//            sCache.put(timeStamp++, data);
//        }
        sCache.put(timeStamp++, data);
//        send(timeStamp-1, false);
        if (isActive){
////            if (needSPS_PPS) {
////                send(timeStamp - sCacheSize, true);
////            } else {
                send(timeStamp-1, false);
//            }
        }
//        Log.d(TAG, "put: bitrate : "+mBitrate+" data length : "+data.length);
    }

    private byte[] getSPS_PPS_SEIFromDefaultCache() {
        return rateAdaptiveEncoder.getSPS_PPS_SEIFromDefaultCache();
    }

    public byte[] getThisSPS_PPS_SEI() {
        if (sps_pps != null) {
            Log.d(TAG, "getThisSPS_PPS: I Have SPS_PPS");
            return sps_pps;
        } else {
            Log.e(TAG, "getThisSPS_PPS: I Don't have SPS_PPS");
            return null;
        }
    }

    public int getmBitrate() {
        return mBitrate;
    }

    private boolean getSPS_PPS_SEI(byte[] data) {
        if (data == null) {
            Log.e(TAG, "getSPS_PPS_SEI: wtf");
            return false;
        }
        byte[] temp = new byte[data.length];
        System.arraycopy(data,0,temp,0,data.length);
        int start = 0;
        int end = 0;
        boolean hasSPS_PPS = false;
        Log.d(TAG, "getSPS_PPS_SEI: "+Arrays.toString(temp));
//        for (int i = 0; !hasSPS_PPS; i++) {
//            Log.d(TAG, "getSPS_PPS_SEI: "+temp[i]+" "+temp[i+1]+" "+temp[i+2]);
//            if ((temp[i] == 0x00) && (temp[i + 1] == 0x00) && (temp[i + 2] == 0x01)) {
//                if ((temp[i + 3] & 0x1f) == 7) {
//                    if (!hasSPS_PPS) {
//                        start = i;
//                        i += 2;
//                    } else {
//                        Log.e(TAG, "getSps_Pps: already has SPS_PPS");
//                    }
//                } else if ((temp[i + 3] & 0x1f) != 8) {
//                    end = i - 1;
//                    hasSPS_PPS = true;
//                }
//            } else if (temp[i] == 0x00 && temp[i + 1] == 0x00 && temp[i + 2] == 0x00 && temp[i + 3] == 0x01) {
//                if ((temp[i + 4] & 0x1f) == 7) {
//                    if (!hasSPS_PPS) {
//                        start = i;
//                        i += 3;
//                    } else {
//                        Log.e(TAG, "getSps_Pps: already has SPS_PPS");
//                    }
//                } else if ((data[i + 4] & 0x1f) != 8) {
//                    end = i - 1;
//                    hasSPS_PPS = true;
//                }
//            }
//        }
//        if (start < end) {
//            Log.d(TAG, "getSps_Pps: strat : " + start + " end : " + end);
//            sps_pps_sei = new byte[data.length];
//            System.arraycopy(temp, 0, sps_pps_sei, 0, temp.length);
////            sei = new byte[data.length - (end - start + 1)];
////            System.arraycopy(data, end + 1, sei, 0, data.length - (end - start + 1));
//            Log.d(TAG, "getSps_Pps: bitrate : " + mBitrate + " res : " + Arrays.toString(sps_pps_sei));
//            Log.d(TAG, "getSps_Pps: data : " + Arrays.toString(temp));
//            return true;
//        } else {
//            return false;
//        }
        sps_pps = new byte[data.length];
        System.arraycopy(data,0,sps_pps,0,data.length);
        hasSPS_PPS = true;
        return true;
    }


    private boolean isIDRFrame (byte[] data) {
        for (int i = 0;;i++){
            if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x01) {
                return (data[i + 3] & 0x1f) == 5;
            } else if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                return (data[i + 4] & 0x1f) == 5;
            }
        }
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setNeedSPS_PPS(boolean needSPS_PPS) {
        this.needSPS_PPS = needSPS_PPS;
    }

    //发送对应时间戳的数据
    private void send (int stamp, boolean needSPS) {
        if (sCache.get(stamp) != null) {
            sendByEchoClient(stamp, sCache.get(stamp), needSPS);
        }
//        Log.d(TAG, "send: send");
    }

    //删除过期数据
    private void deleteStaleData () {
        int key = timeStamp;
        //遍历sCache，删除间隔大于sCacheSize的元素
        Iterator<Map.Entry<Integer, byte[]>> iter = sCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, byte[]> entry =  iter.next();
            if (key - entry.getKey() > sCacheSize) {
                iter.remove();
            }
        }
//        Log.d(TAG, "deleteStaleData: bitrate : "+mBitrate+" cachesize : "+sCache.size());
    }

    //发送数据
    private void sendByEchoClient (int stamp, byte[] data, boolean needSPS) {
//        if (isIDRFrame(data) && needSPS) {
//            byte[] extraMessage = new byte[sps_pps_sei.length + sei.length + 4];
//            System.arraycopy(sps_pps, 0, extraMessage, 4, sps_pps.length);
//            System.arraycopy(sei, 0, extraMessage, sps_pps.length + 4, sei.length);
//            try {
//                client.sendStream_n(extraMessage, extraMessage.length);
//                Log.d(TAG, "sendByEchoClient: I have send sps_pps & sei : "+Arrays.toString(extraMessage));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        byte[] message = generateUDPMessage(stamp, data);
        try {
            client.sendStream_n(message, message.length);
            Log.d(TAG, "sendByEchoClient: let me see the udp message : "+Arrays.toString(message));
//                Log.d(TAG, "sendByEchoClient: data length : "+data.length);
//                Log.d(TAG, "sendByEchoClient: bitrate : "+mBitrate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //将时间戳与编码结果打包，相应地，需要接收端解包
    private byte[] generateUDPMessage (int stamp, byte[] data) {
        byte[] message = new byte[4 + data.length];
        System.arraycopy(int2ByteArray(stamp), 0, message, 0, 4);
        System.arraycopy(data, 0, message, 4, data.length);
        return message;
    }

    //ByteBuffer转Byte[]
    private byte[] decodeValue(ByteBuffer buffer) {
        int len = buffer.limit() - buffer.position();
        byte[] data = new byte[len];
        buffer.get(data);
        return data;
    }

    //int转byte[]
    private static byte[] int2ByteArray(int i){
        byte[] result=new byte[4];
        result[0]=(byte)((i >> 24)& 0xFF);
        result[1]=(byte)((i >> 16)& 0xFF);
        result[2]=(byte)((i >> 8)& 0xFF);
        result[3]=(byte)(i & 0xFF);
        return result;
    }

    public void close() {
        countDownTimer.cancel();
        sCache.clear();
        Log.d(TAG, "close: cancel");
    }
}
