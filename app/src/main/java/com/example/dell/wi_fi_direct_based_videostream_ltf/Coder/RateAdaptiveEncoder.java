package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.dell.wi_fi_direct_based_videostream_ltf.Cache.ServerCache;
import com.upyun.hardware.SoftEncoder;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class RateAdaptiveEncoder {
    private static final String TAG = "RateAdaptiveEncoder";
    private int mWidth;
    private int mHeight;
    private int mFps;
    private boolean isPaused;
    private int activeBitrate;
    private int defaultBitrate;
    private HashMap<Integer, Encoder_Cache> mHashmap = new HashMap<>();

    private boolean useSoftEncdoer;

    public byte[] getSPS_PPS_SEIFromDefaultCache() {
        if (mHashmap.get(defaultBitrate) != null) {
            return mHashmap.get(defaultBitrate).serverCache.getThisSPS_PPS_SEI();
        } else {
            Log.e(TAG, "getSPS_PPSFromDefaultCache: no default SPS_PPS");
            return null;
        }
    }

    //内部类，一个Encoder对应一个ServerCache
    private class Encoder_Cache {
        private SoftEncoder softEncoder;
        private MyCodec myCodec;
        private ServerCache serverCache;
//        private ExecutorService executorService = Executors.newFixedThreadPool(1);

        @RequiresApi(api = Build.VERSION_CODES.M)
        public Encoder_Cache (int width, int height, int bitrate, int fps, int sCacheSize, int cycleTime, RateAdaptiveEncoder rateAdaptiveEncoder) {
            this.serverCache = new ServerCache(sCacheSize, cycleTime, bitrate, rateAdaptiveEncoder);
            if (useSoftEncdoer) {
                this.softEncoder = new SoftEncoder(this.serverCache);
                initSoftEncoder(this.softEncoder, width, height, bitrate, fps);
            } else {
                this.myCodec = new MyCodec("video/avc",720,480,bitrate);
                myCodec.setServerCache(this.serverCache);
                myCodec.startEncoder();
            }
//            Log.d(TAG, "Encoder_Cache: encoder_cache");
        }
        
        public void encode(final byte[] yuvFrame, final int width, final int height, final boolean flip, final int rotate, final long pts) {
//            executorService.execute(new Runnable() {
//                @Override
//                public void run() {
                    synchronized (Encoder_Cache.class){
                        if (useSoftEncdoer) {
                            softEncoder.NV21SoftEncode(yuvFrame, width, height, false, 90, pts, 0, 0, width, height);
                        } else {

                            myCodec.inputFrameToEncoder(yuvFrame);
                        }
                    }
//                }
//            });
//            Log.d(TAG, "encode: encode2");
        }

        public void close() {
            softEncoder.closeSoftEncoder();
            Log.d(TAG, "close: close softencoder");
            serverCache.close();
            Log.d(TAG, "close: close cache");
        }

        public void setActive (boolean active) {
            serverCache.setActive(active);
//            Log.d(TAG, "setActive: set");
        }

        public void setNeedSPS_PPS(boolean needSPS_PPS) {
            this.serverCache.setNeedSPS_PPS(needSPS_PPS);
        }
    }


    public static byte[] nv21ToI420(byte[] data, int width, int height) {
        byte[] ret = new byte[data.length];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);
        ByteBuffer bufferU = ByteBuffer.wrap(ret, total, total / 4);
        ByteBuffer bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4);

        bufferY.put(data, 0, total);
        for (int i=total; i<data.length; i+=2) {
            bufferV.put(data[i]);
            bufferU.put(data[i+1]);
        }

        return ret;
    }

    //构造方法,比特率单位为kbps
    @RequiresApi(api = Build.VERSION_CODES.M)
    public RateAdaptiveEncoder(int width, int height, int bitrate, int fps){

        this.useSoftEncdoer = false;
        mWidth = width;
        mHeight = height;
        mFps = fps;
        isPaused = false;
        activeBitrate = bitrate;
        defaultBitrate = bitrate;
//        mHashmap.put(500, new Encoder_Cache(width, height, 500, fps, 100, 2000));
//        mHashmap.put(1000, new Encoder_Cache(width, height, 1000, fps, 100, 2000));
        mHashmap.put(bitrate, new Encoder_Cache(width, height, bitrate, fps, 100, 2000, this));
        mHashmap.get(bitrate).setActive(true);
//        Log.d(TAG, "RateAdaptiveEncoder: init");
    }

    private void initSoftEncoder(SoftEncoder encoder, int width, int height, int bitrate, int fps){
        if(encoder != null){
            encoder.setEncoderResolution(width, height);
            encoder.setEncoderFps(fps);
            encoder.setEncoderGop(15);
            encoder.setEncoderBitrate(bitrate*1000);
            encoder.setEncoderPreset("veryfast");
            encoder.openSoftEncoder();
            encoder.setInit(true);
//            Log.d(TAG, "initSoftEncoder: init");
        }
    }

    public void encode(byte[] data,long stamp){
        if (isPaused){
//            Log.d(TAG,"Pause");
            return;
        }
        if (data == null || (data.length != mWidth * mHeight * 3 / 2)) {
            Log.w(TAG, "Illegal Data");
            return;
        }

        for (int bitrate : mHashmap.keySet()){

            Encoder_Cache encoder_cache = mHashmap.get(bitrate);
            byte[] yuvFrame = new byte[data.length];
            long pts = stamp;
            System.arraycopy(data, 0, yuvFrame, 0, data.length);
            encoder_cache.encode(yuvFrame, mWidth, mHeight, false, 90, pts);
//            if (encoder_cache.softEncoder != null && encoder_cache.softEncoder.isInit()){
//                byte[] yuvFrame = new byte[data.length];
//                long pts = stamp;
//                System.arraycopy(data, 0, yuvFrame, 0, data.length);
//                Log.d(TAG, "encode: 我们一样吗 : "+System.identityHashCode(yuvFrame));
//                encoder_cache.encode(yuvFrame, mWidth, mHeight, false, 90, pts);
//            }
        }
//        Log.d(TAG, "encode: encode1");
    }

    
    //调整码率，单位kbps
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void adjustBitrate(int bitrate){
        if (mHashmap.get(bitrate) == null){
            isPaused = true;
            mHashmap.get(activeBitrate).setActive(false);
            mHashmap.put(bitrate, new Encoder_Cache(mWidth, mHeight, bitrate, mFps, 100, 2000, this));
            mHashmap.get(bitrate).setActive(true);
            mHashmap.get(bitrate).setNeedSPS_PPS(true);
            activeBitrate = bitrate;
            isPaused = false;
            Log.d(TAG, "adjustBitrate: adjust");
        } else {
            isPaused = true;
            mHashmap.get(activeBitrate).setActive(false);
            mHashmap.get(bitrate).setActive(true);
            mHashmap.get(bitrate).setNeedSPS_PPS(true);
            activeBitrate = bitrate;
            isPaused = false;
            Log.d(TAG, "adjustBitrate: adjust back");
        }
    }

    public void stop() {
        for (int bitrate : mHashmap.keySet()){
            if (mHashmap.get(bitrate) != null){
                mHashmap.get(bitrate).close();
            }
        }
        mHashmap.clear();
    }
}