package com.upyun.hardware;

import android.media.MediaCodec;
import android.os.Build;

import com.example.dell.wi_fi_direct_based_videostream_ltf.Coder.DynamicRateEncoder;
import com.example.dell.wi_fi_direct_based_videostream_ltf.Coder.TestEncoder;

import java.nio.ByteBuffer;


public class SoftEncoder {

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("enc");
    }

    private TestEncoder mTestEncoder;
    private DynamicRateEncoder mDynamicRateEncoder;
//    private int mCount;
    private boolean isInit = false;

    public SoftEncoder(TestEncoder encoder) {
        this.mTestEncoder = encoder;
    }
    public SoftEncoder(DynamicRateEncoder encoder){
        this.mDynamicRateEncoder = encoder;
    }

//    public int getmCount() {
//        return mCount;
//    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public boolean isInit() {
        return isInit;
    }



    public native void setEncoderResolution(int outWidth, int outHeight);
    public native void setEncoderFps(int fps);
    public native void setEncoderGop(int gop);
    public native void setEncoderBitrate(int bitrate);
    public native void setBitrateDynamic(int bitrate);
    public native void setEncoderPreset(String preset);
    public native byte[] NV21ToI420(byte[] yuvFrame, int width, int height, boolean flip, int rotate);
    public native byte[] NV21ToI420(byte[] yuvFrame, int width, int height, boolean flip, int rotate, int cropX, int cropY, int cropWidth, int cropHeight);
    public native byte[] NV21ToNV12(byte[] yuvFrame, int width, int height, boolean flip, int rotate);
    public native byte[] NV21ToNV12(byte[] yuvFrame, int width, int height, boolean flip, int rotate, int cropX, int cropY, int cropWidth, int cropHeight);
    public native int NV21SoftEncode(byte[] yuvFrame, int width, int height, boolean flip, int rotate, long pts);
    public native int NV21SoftEncode(byte[] yuvFrame, int width, int height, boolean flip, int rotate, long pts, int cropX, int cropY, int cropWidth, int cropHeight);
    public native int I420SoftEncode(byte[] yuvFrame, int width, int height, boolean flip, int rotate, long pts);
    public native boolean openSoftEncoder();
    public native void closeSoftEncoder();

    private void onSoftEncodedData(byte[] es, long pts, boolean isKeyFrame) {
        MediaCodec.BufferInfo vebi = new MediaCodec.BufferInfo();
        ByteBuffer bb = ByteBuffer.wrap(es);
        vebi.offset = 0;
        vebi.size = es.length;
        vebi.presentationTimeUs = pts;
        if (Build.VERSION.SDK_INT >= 21) {
            vebi.flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
        } else {
            vebi.flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
        }

        if(mTestEncoder != null){
            mTestEncoder.onEncodedAnnexbFrame(bb,vebi);
        }else if(mDynamicRateEncoder != null){
            mDynamicRateEncoder.onEncodedAnnexbFrame(bb,vebi);
        }
    }
}
