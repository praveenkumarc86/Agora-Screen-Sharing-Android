package io.agora.rtc.ss.app.newInterface;

import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.gl.EglBase;
import io.agora.rtc.mediaio.AgoraSurfaceView;
import io.agora.rtc.mediaio.MediaIO;
import io.agora.rtc.ss.app.R;
import io.agora.rtc.ss.app.newInterface.source.AgoraTexture2DRecord;
import io.agora.rtc.ss.app.newInterface.source.ViewSharingCapturer;
import io.agora.rtc.video.VideoCanvas;


public class RecordService extends Service {

    //RTC的对象
    private RtcEngine mRtcEngine;
    private EglBase.Context mSharedContext;
    private MediaProjection mediaProjection;
    private AgoraTexture2DRecord textureSource;
    private ViewSharingCapturer viewSource;
    private SurfaceView previewSurfaceView;
    private boolean isEnableViewRecord = false;
    private static final String LOG_TAG = "RecordService" + " tjy";
    private boolean running=false;
    private String channelName;
    private int width = 720;
    private int height = 1080;
    private int dpi;
    private View recordView = null;
    private SurfaceReadyListener listener;

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public void setRecordView(View view){
        this.recordView = view;
    }

    //开始record进程
    public boolean startRecord() {
        Log.i("TJY","initRtcEngine start ");

        Log.i("TJY","initRtcEngine over ");
        //初始化texture的设置,并在initSurfaceTexture中显示
        initRtcEngine();
        if(isEnableViewRecord){
            initSurfaceRGBA();
        }else{
            initSurfaceTexture();
        }
        Log.i("TJY","initSurfaceTexture over ");
        //joinChannel
        joinChannel();
        //mediaRecorder.start();
        running = true;
        return true;
    }

    //结束record进程
    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        Log.i("TJY","stopRecord");
        running = false;
/*        if(isEnableViewRecord){
            viewSource.onStop();
            viewSource =null;
        }else{
            textureSource.onStop();
            textureSource =null;
        }*/
        releasTextureSource();
        releaseRGBASource();
        leaveChannel();
        Log.i("TJY","stopRecord over");
        return true;
    }

    public void initRtcEngine() {
        if (mRtcEngine == null) {
            try {
                Log.i("TJY","create mRtcEngine");
                mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.agora_app_id), new IRtcEngineEventHandler() {
                    @Override
                    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                        Log.i(LOG_TAG, "onJoinChannelSuccess" + channel + " " + elapsed);
                    }
                    @Override
                    public void onLeaveChannel (RtcStats stats ) {
                        Log.i(LOG_TAG, "onLeaveChannel");
                    }

                    @Override
                    public void onWarning(int warn) {
                        Log.d(LOG_TAG, "onWarning " + warn);
                    }

                    @Override
                    public void onError(int err) {
                        Log.d(LOG_TAG, "onError " + err);
                    }

                    @Override
                    public void onAudioRouteChanged(int routing) {
                        Log.i(LOG_TAG, "onAudioRouteChanged " + routing);
                    }

                });
            } catch (Exception e) {
                Log.e(LOG_TAG, Log.getStackTraceString(e));

                throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
            }

            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableVideo();
            mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, true);
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        }
    }

    public void setSurfaceReadyListener(SurfaceReadyListener listener) {
        this.listener = listener;
    }

    public void initSurfaceTexture() {
        //初始化source
        //if(source == null) {
            Log.i("TJY", "initSurfaceTexture");
            //mRtcEngine.stopPreview();
            //just a test。。。。。
            //source = new AgoraTexture2DRecord_test(this, this.width, this.height);
            releasTextureSource();
            releaseRGBASource();
            mRtcEngine.stopPreview();
            textureSource = new AgoraTexture2DRecord(this, this.width, this.height, this.dpi, this.mediaProjection);
            //}
            //source.setSize(this.dpi);
            //source.setProjection(mediaProjection);
            mSharedContext =  textureSource.getEglContext();

            //初始化view
            AgoraSurfaceView render = new AgoraSurfaceView(this);
            render.setZOrderOnTop(true);
            render.setZOrderMediaOverlay(true);
            render.init(mSharedContext);
            render.setBufferType(MediaIO.BufferType.TEXTURE);
            //这里是本地的render格式，与remote的render还不一样，这里如果设置的不一样，不影响remote的格式
            render.setPixelFormat(MediaIO.PixelFormat.TEXTURE_OES);

            mRtcEngine.setLocalVideoRenderer(render);
            previewSurfaceView = render;
            listener.surfaceIsReady(previewSurfaceView);
            mRtcEngine.setVideoSource(textureSource);
            //提醒activity初始化preview的部分

            //开始preview
        mRtcEngine.startPreview();
        //}
    }

    public void initSurfaceRGBA(){
        mRtcEngine.stopPreview();
        Log.i("TJY","initSurfaceRGBA");
/*        if(textureSource!=null){
            textureSource.onStop();
            textureSource = null;
        }*/
        releasTextureSource();
        releaseRGBASource();
        viewSource = new ViewSharingCapturer(this.recordView);

        AgoraSurfaceView render = new AgoraSurfaceView(this);
        render.setZOrderOnTop(true);
        render.setZOrderMediaOverlay(true);
        render.init(null);
        render.setBufferType(MediaIO.BufferType.BYTE_BUFFER);
        render.setPixelFormat(MediaIO.PixelFormat.RGBA);
        mRtcEngine.setLocalRenderMode(VideoCanvas.RENDER_MODE_FIT);
        mRtcEngine.setLocalVideoRenderer(render);
        previewSurfaceView = render;
        listener.surfaceIsReady(previewSurfaceView);
        mRtcEngine.setVideoSource(viewSource);
        //提醒activity初始化preview的部分

        mRtcEngine.startPreview();
    }

    public void releasTextureSource(){
        if(textureSource!=null){
            textureSource.sourceRelease();
            textureSource = null;
        }
    }

    public void releaseRGBASource(){
        if(viewSource!=null){
            viewSource= null;
        }

    }

    public void setChannelName(String channelName){
        this.channelName = channelName;
    }

    public void joinChannel() {
        //mRtcEngine.joinChannel(null, "1234321", "", 0);
        mRtcEngine.joinChannel(null, channelName, "", 0);
    }

    public void leaveChannel() {
        mRtcEngine.stopPreview();
        mRtcEngine.leaveChannel();
        //RtcEngine.destroy();
        //mRtcEngine = null;
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }


    public boolean isEnableViewRecord() {
        return isEnableViewRecord;
    }

    public void setEnableViewRecord(boolean enableViewRecord) {
        isEnableViewRecord = enableViewRecord;
        if(running){
            if(isEnableViewRecord){
                initSurfaceRGBA();
            }else{
                initSurfaceTexture();
            }
        }

    }
}