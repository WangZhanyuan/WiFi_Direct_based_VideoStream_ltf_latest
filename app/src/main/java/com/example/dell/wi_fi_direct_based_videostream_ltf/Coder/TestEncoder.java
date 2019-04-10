package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import com.upyun.hardware.SoftEncoder;


public class TestEncoder {
    private final static String MINE_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String TAG = "TestEncoder";
    private MediaCodec mediaCodec;
    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBitrate;
    private byte[] h264;
    private String codecName;
    private byte[] sps_pps;
    public static long startTime;

    private int mPushWidth;
    private int mPushHeight;
    private int mColorFormats;

    private byte[] mRotatedFrameBuffer;
    private byte[] mFlippedFrameBuffer;
    private byte[] mCroppedFrameBuffer;

    private int mVideoTrack;
//    private SrsFlvMuxer mflvmuxer;
    private boolean isStarted = false;
//    private Config config;

    private SoftEncoder softEncoder;

    private int cropX;
    private int cropY;


    private final static int CACHE_BUFFER_SIZE = 8;
    private long number=1000;
    public final static ArrayBlockingQueue<byte []> mOutputDatasQueue = new ArrayBlockingQueue<byte[]>(CACHE_BUFFER_SIZE);
    private EchoClient echoClient=new EchoClient("192.168.49.234");


    public TestEncoder(int width, int height, int bit, int fps) {
        Log.d(TAG,"软编初始化");
        setVideoOptions(width,height,bit,fps);

//        initialize();
    }

//    protected void initialize() {
//        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
//            MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
//            for (String type : mediaCodecInfo.getSupportedTypes()) {
//                if (TextUtils.equals(type, MINE_TYPE)
//                        && mediaCodecInfo.isEncoder()) {
//                    MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo
//                            .getCapabilitiesForType(MINE_TYPE);
//                    for (int format : codecCapabilities.colorFormats) {
//                        if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
//                            codecName = mediaCodecInfo.getName();
//                            mColorFormats = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
//                            return;
//                        } else if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
//                            codecName = mediaCodecInfo.getName();
//                            mColorFormats = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
//                            return;
//                        }
//                    }
//                }
//            }
//        }
//    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setVideoOptions(int width, int height, int bit, int fps) {      //设置参数
//        Log.i(TAG, "userSoft:" + config.useSofeEncode + " bitRate:" + bit
//                + " width:" + width + " height:" + height + " fps:" + fps);
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mBitrate = bit;

        mPushWidth = mWidth;
        mPushHeight = mHeight;
//        mPushHeight = 360;

        if (mWidth != mPushWidth || mHeight != mPushHeight) {
            cropX = (mWidth - mPushWidth) / 2;
            cropY = (mHeight - mPushHeight) / 2;
        }

        softEncoder = new SoftEncoder(this);

        int outWidth;

        int outHeight;

//        if (config.orientation == Config.Orientation.HORIZONTAL) {
            outWidth = mPushWidth;
            outHeight = mPushHeight;

//        } else {
//            outWidth = mPushHeight;
//            outHeight = mPushWidth;
//        }

        softEncoder.setEncoderResolution(outWidth, outHeight);
        softEncoder.setEncoderFps(fps);
        softEncoder.setEncoderGop(15);
        // Unfortunately for some android phone, the output fps is less than 10 limited by the
        // capacity of poor cheap chips even with x264. So for the sake of quick appearance of
        // the first picture on the player, a spare lower GOP value is suggested. But note that
        // lower GOP will produce more I frames and therefore more streaming data flow.
        softEncoder.setEncoderBitrate(bit);
        softEncoder.setEncoderPreset("veryfast");

        softEncoder.openSoftEncoder();


//        mRotatedFrameBuffer = new byte[mPushHeight * mPushWidth * 3 / 2];
//        mFlippedFrameBuffer = new byte[mPushHeight * mPushWidth * 3 / 2];
//        mCroppedFrameBuffer = new byte[mPushHeight * mPushWidth * 3 / 2];

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            try {
//                mediaCodec = MediaCodec.createByCodecName(codecName);
//                MediaFormat mediaFormat = MediaFormat.createVideoFormat(
//                        MINE_TYPE, outWidth, outHeight);
//
//                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit);
//                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
//                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 关键帧间隔时间
//                // 单位s
//                mediaFormat
//                        .setInteger(
//                                MediaFormat.KEY_COLOR_FORMAT,
//                                mColorFormats);
//
//                mediaCodec.configure(mediaFormat, null, null,
//                        MediaCodec.CONFIGURE_FLAG_ENCODE);
//                mediaCodec.start();
//
////                if (PushClient.MODE != PushClient.MODE_AUDIO_ONLY) {
////                    mVideoTrack = mflvmuxer.addTrack(mediaFormat);
////                    mflvmuxer.setVideoResolution(outWidth, outHeight);
////                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public void adjustBitrate(int bitrate) {
//        if (mBitrate == bitrate) {
//            Log.w(TAG, "The bitrate is not changed.");
//            return;
//        }
//
//        synchronized (TestEncoder.class) {
//            if (mediaCodec != null) {
//                MediaFormat mediaFormat = MediaFormat.createVideoFormat(
//                        MINE_TYPE, mPushWidth, mPushHeight);
//
//                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
//                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
//                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 关键帧间隔时间
//                // 单位s
//                mediaFormat
//                        .setInteger(
//                                MediaFormat.KEY_COLOR_FORMAT,
//                                mColorFormats);
//
//                mediaCodec.stop();
//                mediaCodec.configure(mediaFormat, null, null,
//                        MediaCodec.CONFIGURE_FLAG_ENCODE);
//                mediaCodec.start();
//                mBitrate = bitrate;
//            }
//        }
//    }


    public void fireVideo(byte[] data, long stamp) {
        if (data == null || (data.length != mWidth * mHeight * 3 / 2)) {
            Log.w(TAG, "firevideo Illegal data");
            return;
        }
        synchronized (TestEncoder.class) {
//            Log.d(TAG,"start soft encoder!");
//            if (config.orientation == Config.Orientation.HORIZONTAL) {      //水平位置不旋转
                softEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp,
                        cropX, cropY, mPushWidth, mPushHeight);
//            } else {
//                if (config.cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {       //竖直位置旋转画面
//                    softEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 270, stamp,
//                            cropX, cropY, mPushWidth, mPushHeight);
//                } else {
//                    softEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp,
//                            cropX, cropY, mPushWidth, mPushHeight);
//                }
//            }
        }
    }

//    public void stop() {
//        synchronized (TestEncoder.class) {
//            if (mediaCodec != null) {
//                mediaCodec.stop();
//                mediaCodec.release();
//                mediaCodec = null;
//            }
//
//            if (softEncoder != null) {
//                softEncoder.closeSoftEncoder();
//                softEncoder = null;
//            }
//        }
//    }

    public void onEncodedAnnexbFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        if(outputBuffer != null && bufferInfo.size > 0) {
            byte[] buffer = new byte[outputBuffer.remaining()];
            outputBuffer.get(buffer);
            boolean result = mOutputDatasQueue.offer(buffer);//编好的数据进队
            byte[] temp = mOutputDatasQueue.poll();
            if (temp != null && number > 0) {
                try {
                    number--;
                    echoClient.sendStream_n(temp, temp.length);
//                        multicastClient.sendmessage(temp,temp.length);
//                        echoClient2.sendStream_n(temp,temp.length);
//                        echoClient3.sendStream_n(temp,temp.length);
////                        echoClient4.sendStream_n(temp,temp.length);
////                        echoClient5.sendStream_n(temp,temp.length);
////                        echoClient6.sendStream_n(temp,temp.length);
////                        multicastClient.sendmessage(temp,temp.length);
//                    Log.d(TAG, "发送的数据" + number);
//                    Log.d(TAG, "编码后大小:" + bufferInfo.size);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    Log.d(TAG, "编码压缩后的数据"+temp.length);
            } else {
                //Log.d(TAG, "onOutputBufferAvailable: 发送完毕！");
            }
            if (!result) {
                Log.d(TAG, "Offer to queue failed, queue in full state");
            }
        }
    }

    public void stop() {
        synchronized (TestEncoder.class) {

            if (softEncoder != null) {
                softEncoder.closeSoftEncoder();
                softEncoder = null;
            }
        }
    }



    public void changebitrate(int bit){
        if(softEncoder != null){
            softEncoder.setBitrateDynamic(bit);
            Log.d(TAG,"调用了setBitrateDynamic，调整为" + bit);
        }
    }

}
