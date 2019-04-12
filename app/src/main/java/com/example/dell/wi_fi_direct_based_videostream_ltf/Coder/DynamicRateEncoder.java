package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;

import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoClient;
import com.upyun.hardware.SoftEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import android.media.MediaCodec;
import android.util.Log;

public class DynamicRateEncoder {
    private static final String TAG = "DynamicRateEncoder";
    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBitrate;

    private SoftEncoder mSoftEncoder;
    private SoftEncoder mSoftEncoder2;

    private final static int CACHE_BUFFER_SIZE = 8;
    public final static ArrayBlockingQueue<byte []> mOutputDatasQueue = new ArrayBlockingQueue<byte[]>(CACHE_BUFFER_SIZE);
    private EchoClient echoClient;

    public DynamicRateEncoder(int width, int height, int bitrate, int fps){
        mWidth = width;
        mHeight = height;
        mBitrate = bitrate;
        mFps = fps;
        mSoftEncoder = new SoftEncoder(this,1);

        initSoftEncoder(mSoftEncoder,mWidth, mHeight, mBitrate, mFps);
        echoClient = new EchoClient("192.168.49.234");

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
            Log.d(TAG,"SoftEncoder initialized! Bitrate = "+bitrate);
        }else{
            Log.e(TAG,"init SoftEncoder failed!");
        }
    }

    public void encode(byte[] data,long stamp){
        if (data == null || (data.length != mWidth * mHeight * 3 / 2)) {
            Log.w(TAG, "Illegal data");
            return;
        }
        synchronized (DynamicRateEncoder.class) {
            Log.d(TAG,"编码前数据大小:"+data.length);
            if(mSoftEncoder != null){
                mSoftEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
            }
            if(mSoftEncoder2 != null){
                mSoftEncoder2.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp, 0, 0, mWidth, mHeight);
            }
        }
    }

    public void onEncodedAnnexbFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, int Flag) {
        Log.d(TAG,"This frame is from encoder "+Flag);
        if(mSoftEncoder != null && Flag == 2){
            mSoftEncoder.closeSoftEncoder();
            mSoftEncoder = null;
            return;
        }
        enQueue(outputBuffer, bufferInfo);
        deQueue();
    }

    public void stop() {
        synchronized (DynamicRateEncoder.class) {
            if (mSoftEncoder != null) {
                mSoftEncoder.closeSoftEncoder();
                mSoftEncoder = null;
            }
            if(mSoftEncoder2 != null){
                mSoftEncoder2.closeSoftEncoder();
                mSoftEncoder2 = null;
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
        mSoftEncoder2 = new SoftEncoder(this,2);
        initSoftEncoder(mSoftEncoder2, mWidth, mHeight, bitrate, mFps);
    }

}
