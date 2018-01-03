package io.agora.rtc.ss.app.newInterface.source;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import io.agora.rtc.mediaio.IVideoFrameConsumer;
import io.agora.rtc.mediaio.IVideoSource;
import io.agora.rtc.video.AgoraVideoFrame;

public class ViewSharingCapturer implements IVideoSource {
    private static final String TAG = ViewSharingCapturer.class.getSimpleName();
    private static final int FPS = 10;
    private IVideoFrameConsumer mIVideoFrameConsumer;
    private boolean mCapturerIsPaused = false;
    private ViewCaptureSetting mViewCapturerSettings;
    private View mContentView;
    private View mTtestView;
    private Handler mFrameProducerHandler;
    private long mFrameProducerIntervalMillis = 1000 / FPS;
    private Runnable mFrameProducer = new Runnable() {
        @Override
        public void run() {
            if (mContentView != null) {
                Log.i(TAG, "setDrawingCacheEnabled");
                mContentView.setDrawingCacheEnabled(true);
                mContentView.buildDrawingCache();
                Bitmap bmp = mContentView.getDrawingCache();

                byte[] byteTemp = getRGBFromBMP(bmp);

                if (bmp != null) {

                    Log.i(TAG, "try to push rgba......");
                    mIVideoFrameConsumer.consumeByteArrayFrame(
                            byteTemp
                            , AgoraVideoFrame.FORMAT_RGBA, bmp.getWidth(), bmp.getHeight(), 0, System.currentTimeMillis());
                    Log.i(TAG, "try to push rgba over.....");
                }
                mContentView.setDrawingCacheEnabled(false);
                if (!mCapturerIsPaused) {
                    Log.i(TAG, "try to postDelayed rgba again......");
                    mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
                }
            }

        }
    };

    public ViewSharingCapturer(View view) {
        mContentView = view;
        mFrameProducerHandler = new Handler();
        mViewCapturerSettings = new ViewCaptureSetting();
        mViewCapturerSettings.setFps(FPS);
        mViewCapturerSettings.setFormat(AgoraVideoFrame.FORMAT_RGBA);
    }


    public void setTestView(View view) {
        Log.i(TAG, "setTestView");
        this.mTtestView = view;
    }


    public boolean onInitialize(IVideoFrameConsumer var1) {
        try {
            Log.i(TAG, "onInitialize");
            this.mIVideoFrameConsumer = var1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean onStart() {
        try {
            mCapturerIsPaused = false;
            Log.i(TAG, "try to onStart mFrameProducerIntervalMillis");
            mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
        return true;
    }

    public void onStop() {
        Log.i(TAG, "ViewSharingCapturer onStop");
        mCapturerIsPaused = true;
        mFrameProducerHandler.removeCallbacks(mFrameProducer);
    }

    public void onDispose() {
        Log.i(TAG, "ViewSharingCapturer onDispose");
        mCapturerIsPaused = true;
        mFrameProducerHandler.removeCallbacks(mFrameProducer);
    }

    public int getBufferType() {
        return mViewCapturerSettings.getFormat();
    }


    public static byte[] getRGBFromBMP(Bitmap bmp) {

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        byte[] pixels = new byte[w * h * 4]; // Allocate for RGB

        int k = 0;

        for (int x = 0; x < h; x++) {
            for (int y = 0; y < w; y++) {
                int color = bmp.getPixel(y, x);
/*                pixels[k * 4] = (byte) Color.red(color);
                pixels[k * 4 + 1] = (byte) Color.green(color);
                pixels[k * 4 + 2] = (byte) Color.blue(color);
                pixels[k * 4 + 3] = (byte) Color.alpha(color);*/
                pixels[k * 4 + 3] = (byte) Color.red(color);
                pixels[k * 4 + 2] = (byte) Color.green(color);
                pixels[k * 4 + 1] = (byte) Color.blue(color);
                pixels[k * 4 + 0] = (byte) Color.alpha(color);
                k++;
            }
        }

        return pixels;
    }
}