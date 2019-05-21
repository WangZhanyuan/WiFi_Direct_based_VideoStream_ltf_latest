package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;

import com.example.dell.wi_fi_direct_based_videostream_ltf.Cache.ServerCache;
import com.upyun.hardware.SoftEncoder;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

public class RateAdaptiveEncoder {
    private static final String TAG = "RateAdaptiveEncoder";
    private int mWidth;
    private int mHeight;
    private int mFps;
    private boolean isPaused;
    private int activeBitrate;
    private int defaultBitrate;
    private HashMap<Integer, Encoder_Cache> mHashmap = new HashMap<>();

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
        private ServerCache serverCache;
//        private ExecutorService executorService = Executors.newFixedThreadPool(1);

        public Encoder_Cache (int width, int height, int bitrate, int fps, int sCacheSize, int cycleTime, RateAdaptiveEncoder rateAdaptiveEncoder) {
            this.serverCache = new ServerCache(sCacheSize, cycleTime, bitrate, rateAdaptiveEncoder);
            this.softEncoder = new SoftEncoder(this.serverCache);
            initSoftEncoder(this.softEncoder, width, height, bitrate, fps);
//            Log.d(TAG, "Encoder_Cache: encoder_cache");
        }
        
        public void encode(final byte[] yuvFrame, final int width, final int height, final boolean flip, final int rotate, final long pts) {
//            executorService.execute(new Runnable() {
//                @Override
//                public void run() {
                    synchronized (Encoder_Cache.class){
                        softEncoder.NV21SoftEncode(yuvFrame, width, height, false, 90, pts, 0, 0, width, height);
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

    //构造方法,比特率单位为kbps
    public RateAdaptiveEncoder(int width, int height, int bitrate, int fps){
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

    private void initSoftEncoder(SoftEncoder encoder,int width, int height, int bitrate, int fps){
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
            if (encoder_cache.softEncoder != null && encoder_cache.softEncoder.isInit()){
                byte[] yuvFrame = new byte[data.length];
                long pts = stamp;
                System.arraycopy(data, 0, yuvFrame, 0, data.length);
                Log.d(TAG, "encode: 我们一样吗 : "+System.identityHashCode(yuvFrame));
                encoder_cache.encode(yuvFrame, mWidth, mHeight, false, 90, pts);
            }
        }
//        Log.d(TAG, "encode: encode1");
    }

    
    //调整码率，单位kbps
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