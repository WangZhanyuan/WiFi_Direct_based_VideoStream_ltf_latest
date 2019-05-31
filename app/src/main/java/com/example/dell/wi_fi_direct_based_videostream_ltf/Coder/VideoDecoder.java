package com.example.dell.wi_fi_direct_based_videostream_ltf.Coder;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

//import com.example.dell.wi_fi_direct_based_videostream_ltf.Cache.ClientCache;
import com.example.dell.wi_fi_direct_based_videostream_ltf.Multicast.MulticastServer;
import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoServer;
import com.example.dell.wi_fi_direct_based_videostream_ltf.Multicast.MulticastClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class use for Decode Video Frame Data and show to SurfaceTexture
 * Created by zj on 2018/7/29 0029.
 */
public class VideoDecoder {
    private final static String TAG = "VideoDecoder";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec  mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface     mSurface;
    private int         mViewWidth;
    private int         mViewHeight;
    private int number=0;
    private Handler mVideoDecoderHandler;
    private HandlerThread mVideoDecoderHandlerThread = new HandlerThread("VideoDecoder");
    private EchoServer echoServer;
    private MulticastServer multicastServer;
//    private ClientCache clientCache = new ClientCache(100);

    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int id) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(id);
            inputBuffer.clear();

            byte [] dataSources = null;
//            dataSources = clientCache.get();
//            if(true) {
//                dataSources = mVideoEncoder.pollFrameFromEncoder();
            byte[] tempData = echoServer.pollFramedata();
            Log.d(TAG, "onInputBufferAvailable: 我从echoserver拿到数据了吗？"+Arrays.toString(tempData));
            if (tempData != null) {
                byte[] data = new byte[tempData.length-4];
                System.arraycopy(tempData, 4, data , 0, tempData.length - 4);
                dataSources = data;
            }
//            dataSources=echoServer.pollFramedata();
//            dataSources=multicastServer.pollFramedata();
//                if (dataSources!=null)
//                Log.d(TAG, "onInputBufferAvailable: 解码器缓冲区可以用了！"+Arrays.toString(dataSources));

//            }
            int length = 0;
            if(dataSources != null) {
                inputBuffer.put(dataSources);
                length = dataSources.length;
//                number++;
//                Log.d(TAG, "onInputBufferAvailable:接收到了 "+number);
            }
            mediaCodec.queueInputBuffer(id,0, length,System.nanoTime()/1000,0);
//            Log.d(TAG, "onInputBufferAvailable: let me see input buffer : "+Arrays.toString(dataSources));
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
            if(mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0){
                byte [] buffer = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
//                Log.d(TAG, "onOutputBufferAvailable: 解码器缓冲区可以用了！");
            }
            mMediaCodec.releaseOutputBuffer(id, true);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "------> onError");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, "------> onOutputFormatChanged");
        }
    };//MediaCodec Callback 结束！



    public VideoDecoder(String mimeType, Surface surface, int viewwidth, int viewheight
//            ,byte[] sps,byte[] pps
    ){
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mimeType);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        if(surface == null){
            return;
        }

        this.mViewWidth  = viewwidth;
        this.mViewHeight = viewheight;
        this.mSurface = surface;



        mMediaFormat = MediaFormat.createVideoFormat(mimeType, mViewWidth, mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1920*1080);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16*32);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
//            mMediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
//            mMediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
        }catch (Exception e){e.printStackTrace();}

    }

    public void setechoServer(EchoServer echoServer,MulticastServer multicastServer){
//        clientCache.setechoServer(echoServer, multicastServer);
        this.echoServer=echoServer;
        this.multicastServer=multicastServer;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startDecoder(){
        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new Handler(mVideoDecoderHandlerThread.getLooper());
        if(mMediaCodec != null && mSurface != null){
            try{
                mMediaCodec.setCallback(mCallback, mVideoDecoderHandler);
                mMediaCodec.configure(mMediaFormat, mSurface,null,0);
                Log.d(TAG,"MediaFormat:" + mMediaFormat.toString());
                mMediaCodec.start();
//                Log.d(TAG,"Mediacodec Start By Baymax");
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec is init correct");
        }
    }

    public void stopDecoder(){
        if(mMediaCodec != null){
            mMediaCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release(){
        if(mMediaCodec != null){
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    public void reset() {
        if (mMediaCodec != null) {
            mVideoDecoderHandlerThread.quit();
            mMediaCodec.stop();
        }
    }
}

