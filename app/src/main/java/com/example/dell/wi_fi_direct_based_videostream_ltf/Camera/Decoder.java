package com.example.dell.wi_fi_direct_based_videostream_ltf.Camera;

import android.graphics.PixelFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.dell.wi_fi_direct_based_videostream_ltf.Coder.VideoDecoder;
import com.example.dell.wi_fi_direct_based_videostream_ltf.Multicast.MulticastServer;
import com.example.dell.wi_fi_direct_based_videostream_ltf.R;
import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoServer;

public class Decoder extends AppCompatActivity {
    public static final String TAG = CameraActivity.class.getSimpleName();
    private EchoServer server;
    private MulticastServer multicastServer;
    private SurfaceHolder mSurfaceHolder;
    private final String MIME_TYPE = "video/avc";
    private SurfaceView surfaceView;
    private Button start;
    private Button reset;
    private VideoDecoder videoDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
        surfaceView = (SurfaceView) findViewById(R.id.sfvDecodeView);
        start = (Button)findViewById(R.id.startdecode);
        reset = (Button)findViewById(R.id.reset);
        mSurfaceHolder = surfaceView.getHolder();

        start.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                try {
                    server = new EchoServer();
                    new Thread(server).start();
                    multicastServer = new MulticastServer();
                    new Thread(multicastServer).start();
                    Log.d(TAG, "onClick: 这是UDP 服务端！");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSurfaceHolder.getSurface() != null) {
                                Log.d(TAG, "run: surface2" + mSurfaceHolder.getSurface().toString());
                                startdecode(MIME_TYPE, mSurfaceHolder.getSurface(), 640, 480, server, multicastServer);
                                Log.d(TAG, "run: 解码开始！");
                            } else {
                                Log.d(TAG, "run: surface2为空无法解码！");
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoDecoder.reset();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.setZOrderMediaOverlay(true);//把控件放在窗口最顶层
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(mSurfaceHolderCallback1);
        Log.d(TAG, "onResume: onResume 执行了");
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback1=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startdecode(String mimeType, Surface surface, int viewwidth, int viewheight, EchoServer echoServer,MulticastServer multicastServer){
        videoDecoder = new VideoDecoder(mimeType,surface,viewwidth,viewheight);
//       Log.d(TAG, "startdecode: sps"+Arrays.toString(encoder.mSps));
//        Log.d(TAG, "startdecode: pps"+Arrays.toString(encoder.mPps));
        videoDecoder.setechoServer(echoServer,multicastServer);
        videoDecoder.startDecoder();
    }

}
