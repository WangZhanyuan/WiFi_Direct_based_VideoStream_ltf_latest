package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;

import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoClient;
import com.upyun.hardware.SoftEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import android.media.MediaCodec;
import android.util.Log;

public class DynamicRateEncoder {
    private static final String TAG = "DynamicRateEncoder";

    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBitrate;
    private boolean isPaused;
    private int activeBitrate;

    private final static int CACHE_BUFFER_SIZE = 8;
    public final static ArrayBlockingQueue<byte []> mOutputDatasQueue = new ArrayBlockingQueue<byte[]>(CACHE_BUFFER_SIZE);
    private EchoClient echoClient;

    private HashMap<Integer ,SoftEncoder> mEncoderHashmap = new HashMap<>();
//    private int mCount;
//    private ArrayList<SoftEncoder> mEncoderList = new ArrayList<>();

    //构造方法,比特率单位为kbps
    public DynamicRateEncoder(int width, int height, int bitrate, int fps){
        mWidth = width;
        mHeight = height;
        mBitrate = bitrate;
        mFps = fps;
        echoClient = new EchoClient("192.168.49.234");
        isPaused = false;
        activeBitrate = mBitrate;
//        mCount = 0;

        mEncoderHashmap.put(mBitrate,new SoftEncoder(this));
        initSoftEncoder(mEncoderHashmap.get(bitrate),mWidth, mHeight, mBitrate*1000, mFps);
//        mEncoderList.add(new SoftEncoder(this,++mCount));
//        initSoftEncoder(mEncoderList.get(0),mWidth, mHeight, mBitrate, mFps);
        Log.d(TAG,TAG+" initialized!");
    }

    public void initSoftEncoder(SoftEncoder encoder,int width, int height, int bitrate, int fps){
        if(encoder != null){
            encoder.setEncoderResolution(width, height);
            encoder.setEncoderFps(fps);
            encoder.setEncoderGop(15);
            encoder.setEncoderBitrate(bitrate);
            encoder.setEncoderPreset("veryfast");
            encoder.openSoftEncoder();
            encoder.setInit(true);
            Log.d(TAG,"SoftEncoder initialized! Bitrate = "+bitrate);
        }
    }

    public synchronized void encode(byte[] data,long stamp){
        if (isPaused){
            Log.d(TAG,"Paused");
            return;
        }
        if (data == null || (data.length != mWidth * mHeight * 3 / 2)) {
            Log.w(TAG, "Illegal data");
            return;
        }

        for (int bitrate : mEncoderHashmap.keySet()){
            if (bitrate == activeBitrate){
                Log.d(TAG,"ActiveBitrate is "+bitrate);
                SoftEncoder activeSoftEncoder = mEncoderHashmap.get(bitrate);
                if (activeSoftEncoder != null && activeSoftEncoder.isInit()){
                    //默认竖直方向，旋转90°
                    activeSoftEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
                }
            }
        }
//        mEncoderList.getLast().NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//        Iterator<SoftEncoder> iterator = mEncoderList.iterator();
//        while (iterator.hasNext()) {
//            SoftEncoder mEncoder = iterator.next();
//            if(mEncoder != null){
//                Log.d(TAG, "给" + mEncoder.getmCount() + "号编码器喂数据...");
//                mEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//            }
//        }
//        //下面这段是能用的hhh
//        for (int i = 0; i < mCount; i++) {
//            SoftEncoder mEncoder = mEncoderList.get(i);
//            if(mEncoder != null && mEncoder.isInit()){
//                Log.d(TAG, "给" + mEncoder.getmCount() + "号编码器喂数据...");
//                mEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//            }
//        }
//        for(SoftEncoder mEncoder : mEncoderList){
//            if(mEncoder != null){
//                Log.d(TAG, "给" + mEncoder.getmCount() + "号编码器喂数据...");
//                mEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//            }
//        }
    }

    //尝试过关闭编码器，但一直崩溃，出现signal 11错误，怀疑jni写的有问题，干脆不关了
    public void onEncodedAnnexbFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        sendByEchoClient(outputBuffer, bufferInfo);
//        if (count < mCount){
////            Log.d(TAG,"无脑入队ojbk!"+count);
////        }
////        if (count == mCount){
////            if (this.closeFlag) {
////                int i = 0;
////                for (SoftEncoder mEncoder : mEncoderList) {
////                    if (mEncoder != null && mEncoder.getmCount() != count) {
////                        mEncoder.closeSoftEncoder();
////                        Log.d(TAG, "我关闭了" + mEncoder.getmCount() + "号编码器！！");
////                        mEncoderList.set(i, null);
////                    }
////                    i++;
////                }
////                this.closeFlag = false;
////            }
////            Log.d(TAG,"有脑入队ojbk!"+count);
////        }
//
//
////        if(count == mCount - 1 && ){
////            synchronized (DynamicRateEncoder.class) {
////                for (SoftEncoder mEncoder : mEncoderList) {
////                    if (mEncoder.getmCount() != count) {
////                        mEncoder.closeSoftEncoder();
////                        Log.d(TAG,"我关闭了"+mEncoder.getmCount()+"号编码器！！");
////                    }
////                }
////            }
//////            while(mEncoderList.getFirst().getmCount()!=count){
//////                mEncoderList.getFirst().closeSoftEncoder();
//////                 = null;
//////                mEncoderList.removeFirst();
//////            }
////            return;
////        }
    }

    public void sendByEchoClient(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo){
        if(outputBuffer != null && bufferInfo.size > 0) {
            byte[] buffer = new byte[outputBuffer.remaining()];
            outputBuffer.get(buffer);
            boolean result = mOutputDatasQueue.offer(buffer);
            byte[] temp = mOutputDatasQueue.poll();
            if (temp != null) {
                try {
                    echoClient.sendStream_n(temp, temp.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "编码后数据大小："+temp.length);
            }
            if (!result) {
                Log.d(TAG, "Offer to queue failed, queue in full state");
            }
        }
    }

    //调整码率，单位kbps
    public void adjustBitrate(int bitrate){
        if (mEncoderHashmap.get(bitrate) == null){
            isPaused = true;
            Log.d(TAG,"isPaused");
            mEncoderHashmap.put(bitrate,new SoftEncoder(this));
            initSoftEncoder(mEncoderHashmap.get(bitrate), mWidth, mHeight, bitrate*1000, mFps);
            mEncoderHashmap.put(activeBitrate,null);
            activeBitrate = bitrate;
            isPaused = false;
            Log.d(TAG,"play!");
        }
////        if (mCount){
////            if (this.closeFlag) {
//        int i = 0;
//        for (SoftEncoder mEncoder : mEncoderList) {
//            if (mEncoder != null) {
////                mEncoder.closeSoftEncoder();
//                mEncoderList.set(i, null);
//                Log.d(TAG, "我关闭了" + mEncoder.getmCount() + "号编码器！！"); //实际上没关只是设为null。。不知为什么一关就出错
//            }
//            i++;
//        }
////                this.closeFlag = false;
////            }
//        Log.d(TAG,"全都关了ojbk!"+mCount);
//        System.gc();
//        Log.d(TAG,"gc");
////        }
//
//        mEncoderList.add(new SoftEncoder(this, ++mCount));
////        this.closeFlag = true;
//        Log.d(TAG, "添加了编号为" + mCount + "的编码器,但尚未初始化");
//        mCount++;
    }

    public void stop() {
        for (SoftEncoder mSoftEncoder : mEncoderHashmap.values()){
            if (mSoftEncoder != null){
                //默认竖直方向，旋转90°
                mSoftEncoder.closeSoftEncoder();
            }
        }
        Log.d(TAG,TAG+" stopped!");
    }
}
