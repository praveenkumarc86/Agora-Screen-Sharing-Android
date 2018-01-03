/*
 * Copyright (c) 2017  www.agora.io
 *
 * All Rights Reserved.
 */
package io.agora.rtc.ss.app.newInterface.source;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import io.agora.rtc.mediaio.TextureSource;

import static io.agora.rtc.mediaio.MediaIO.PixelFormat.TEXTURE_OES;


//这里可能是要学习他的过程.....即，改为屏幕录制的模式.....
public class AgoraTexture2DRecord extends TextureSource {
    private static final String TAG = AgoraTexture2DRecord.class.getSimpleName();
    private Context mContext;
    private Camera camera;
    private Camera.CameraInfo info;
    private int width = 320;
    private int height = 240;
    private int dpi;
    private int rotation = 0;
    //和record相关

    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;

    public AgoraTexture2DRecord(Context context, int width, int height, int dpi, MediaProjection mediaProjection) {
        super(null, width, height);
        Log.i(TAG, "init AgoraTexture2DRecord");
        this.width = width;
        this.height = height;
        this.mContext = context;
        this.dpi = dpi;
        this.mediaProjection = mediaProjection;
    }

    //在这里处理回调的每一帧
    //这里绘制的surface是在哪里绘制到本地的？
    @Override
    public void onTextureFrameAvailable(int oesTextureId, float[] transformMatrix, long timestampNs) {
        super.onTextureFrameAvailable(oesTextureId, transformMatrix, timestampNs);
        //可以在这里，对oexTextureid进行处理
        Log.i(TAG, "try to mConsurer_2。。。。");
        if (mConsumer != null && mConsumer.get() != null) {
            mConsumer.get().consumeTextureFrame(oesTextureId, TEXTURE_OES.intValue(), mWidth, mHeight, rotation, System.currentTimeMillis(), transformMatrix);
        }
    }

    //在这里绑定camera
    @Override
    protected boolean onCapturerOpened() {
        Log.i(TAG, "onCapturerOpened");
        createVirtualDisplay();
        return true;
    }

    @Override
    protected boolean onCapturerStarted() {
        //camera.startPreview();
        Log.i(TAG, "onCapturerStarted");
        return true;
    }

    @Override
    protected void onCapturerStopped() {
        //camera.stopPreview();
        Log.i(TAG, "onCapturerStopped");
    }

    @Override
    protected void onCapturerClosed() {

        Log.i(TAG, "onCapturerClosed");
    }

    public void sourceRelease() {
        Log.i(TAG, "sourceRelease");
        releaseProjection();
        release();
    }


    // record operations
    private void createVirtualDisplay() {
        Log.i(TAG, "createVirtualDisplay:" + mediaProjection);
        Surface inputSurface = new Surface(getSurfaceTexture());
        if (virtualDisplay == null) {
            virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, inputSurface, null, null);
        }
    }

    public void releaseProjection() {
        Log.i(TAG, "releaseProjection");
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        virtualDisplay = null;
    }

}
