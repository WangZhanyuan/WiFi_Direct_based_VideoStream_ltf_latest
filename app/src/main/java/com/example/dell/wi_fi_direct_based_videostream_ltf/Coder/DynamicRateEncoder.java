package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;

import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoClient;
import com.upyun.hardware.SoftEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

import android.media.MediaCodec;
import android.util.Log;

public class DynamicRateEncoder {
    private static final String TAG = "DynamicRateEncoder";
    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBitrate;
    private boolean closeFlag = false;

    private boolean isPaused;

    private final static int CACHE_BUFFER_SIZE = 8;
    public final static ArrayBlockingQueue<byte []> mOutputDatasQueue = new ArrayBlockingQueue<byte[]>(CACHE_BUFFER_SIZE);
    private EchoClient echoClient;

    ArrayList<SoftEncoder> mEncoderList = new ArrayList<>();
    private int mCount;

    public DynamicRateEncoder(int width, int height, int bitrate, int fps){
        mWidth = width;
        mHeight = height;
        mBitrate = bitrate;
        mFps = fps;
        echoClient = new EchoClient("192.168.49.234");
        mCount = 0;
        isPaused = false;
        mEncoderList.add(new SoftEncoder(this,++mCount));
        initSoftEncoder(mEncoderList.get(0),mWidth, mHeight, mBitrate, mFps);
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
        }else{
            Log.e(TAG,"init SoftEncoder failed!");
        }
    }

    public synchronized void encode(byte[] data,long stamp){
        if (isPaused){
            Log.d(TAG,"罢工！");
            return;
        }

        if (data == null || (data.length != mWidth * mHeight * 3 / 2)) {
            Log.w(TAG, "Illegal data");
            return;
        }
//      Log.d(TAG,"编码前数据大小:"+data.length);
//        Log.d(TAG,"编码器列表长度："+mEncoderList.size());
//        Log.d(TAG,"编码器队尾编号："+mEncoderList.get(mEncoderList.size() - 1).getmCount());
//      mEncoderList.getLast().f();
//      mEncoderList.getLast().NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//        Iterator<SoftEncoder> iterator = mEncoderList.iterator();
//        while (iterator.hasNext()) {
//            SoftEncoder mEncoder = iterator.next();
//            if(mEncoder != null){
//                Log.d(TAG, "给" + mEncoder.getmCount() + "号编码器喂数据...");
//                mEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//            }
//        }
        for (int i = 0; i < mCount; i++) {
            SoftEncoder mEncoder = mEncoderList.get(i);
            if(mEncoder != null && mEncoder.isInit()){
                Log.d(TAG, "给" + mEncoder.getmCount() + "号编码器喂数据...");
                mEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
            }
        }
//        for(SoftEncoder mEncoder : mEncoderList){
//            if(mEncoder != null){
//                Log.d(TAG, "给" + mEncoder.getmCount() + "号编码器喂数据...");
//                mEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
//            }
//        }
    }

    public void onEncodedAnnexbFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, int count) {
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
        enQueue(outputBuffer, bufferInfo);
        deQueue();
        Log.d(TAG,"This Frame Is From "+count);
    }

    public void stop() {
        for(SoftEncoder mSoftEncoder : mEncoderList){
            if(mSoftEncoder != null){
                mSoftEncoder.closeSoftEncoder();
                mSoftEncoder = null;
            }
        }
        Log.d(TAG,TAG+" stopped!");
    }

    public void enQueue(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo){
        if(outputBuffer != null && bufferInfo.size > 0) {
            byte[] buffer = new byte[outputBuffer.remaining()];
            outputBuffer.get(buffer);
            boolean result = mOutputDatasQueue.offer(buffer);
            if (!result) {
                Log.d(TAG, "Offer to queue failed, queue in full state");
            }
        }
    }

    public void deQueue(){
        byte[] temp = mOutputDatasQueue.poll();
        if (temp != null) {
            try {
                echoClient.sendStream_n(temp, temp.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "编码后数据大小："+temp.length);
        }
    }

    public void adjustBitrate(int bitrate){

        isPaused = true;
        Log.d(TAG,"isPaused");

//        if (mCount){
//            if (this.closeFlag) {
        int i = 0;
        for (SoftEncoder mEncoder : mEncoderList) {
            if (mEncoder != null) {
//                mEncoder.closeSoftEncoder();
                mEncoderList.set(i, null);
                Log.d(TAG, "我关闭了" + mEncoder.getmCount() + "号编码器！！"); //实际上没关只是设为null。。不知为什么一关就出错
            }
            i++;
        }
//                this.closeFlag = false;
//            }
        Log.d(TAG,"全都关了ojbk!"+mCount);
        System.gc();
        Log.d(TAG,"gc");
//        }

        mEncoderList.add(new SoftEncoder(this, ++mCount));
//        this.closeFlag = true;
        Log.d(TAG, "添加了编号为" + mCount + "的编码器,但尚未初始化");
        initSoftEncoder(mEncoderList.get(mEncoderList.size() - 1), mWidth, mHeight, bitrate, mFps);
        isPaused = false;
        Log.d(TAG,"play!");
//        mCount++;
    }
}
