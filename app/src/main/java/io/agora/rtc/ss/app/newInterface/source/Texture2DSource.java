package io.agora.rtc.ss.app.newInterface.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import io.agora.rtc.gl.EglBase;
import io.agora.rtc.mediaio.IVideoFrameConsumer;
import io.agora.rtc.mediaio.IVideoSource;
import io.agora.rtc.ss.app.R;
import io.agora.rtc.utils.ThreadUtils;
import io.agora.rtc.video.AgoraVideoFrame;

/**
 * Created by Yao Ximing on 2017/12/12.
 */

public class Texture2DSource implements IVideoSource {
    private final static String TAG = Texture2DSource.class.getSimpleName();

    private IVideoFrameConsumer mIVideoFrameConsumer;
    private static WeakReference<Context> mContext;
    private EglBase.Context mEglContext;
    private HandlerThread thread;
    private Handler handler;
    private EglBase eglBase;
    private float mMatrix[] = new float[16];
    private int texId;
    private int mWidth;
    private int mHeight;

    private boolean mRenderStopped;

    public static Texture2DSource create(Context context, final String threadName,
                                         final EglBase.Context sharedContext) {
        mContext = new WeakReference<Context>(context);

        final HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        final Handler handler = new Handler(thread.getLooper());

        // The onFrameAvailable() callback will be executed on the SurfaceTexture ctor thread. See:
        // http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/5.1.1_r1/android/graphics/SurfaceTexture.java#195.
        // Therefore, in order to control the callback thread on API lvl < 21, the SurfaceTextureHelper
        // is constructed on the |handler| thread.
        return ThreadUtils.invokeAtFrontUninterruptibly(handler, new Callable<Texture2DSource>() {
            @Override
            public Texture2DSource call() {
                try {
                    return new Texture2DSource(sharedContext, handler);
                } catch (RuntimeException e) {
                    Log.e(TAG, threadName + " create failure", e);
                    return null;
                }
            }
        });
    }

    private Texture2DSource(EglBase.Context sharedContext, Handler handler) {
        if (handler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("SurfaceTextureHelper must be created on the handler thread");
        }

        this.handler = handler;

        eglBase = EglBase.create(sharedContext, EglBase.CONFIG_PIXEL_BUFFER);
        mEglContext = eglBase.getEglBaseContext();

        try {
            eglBase.createDummyPbufferSurface();
            eglBase.makeCurrent();
        } catch (RuntimeException ex) {
            eglBase.release();
            handler.getLooper().quit();
            throw ex;
        }

        createTexture();

        android.opengl.Matrix.setIdentityM(mMatrix, 0);
    }

    @Override
    public boolean onInitialize(IVideoFrameConsumer iVideoFrameConsumer) {
        this.mIVideoFrameConsumer = iVideoFrameConsumer;
        mRenderStopped = false;
        return true;
    }

    @Override
    public boolean onStart() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    if (mRenderStopped) return;

                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mIVideoFrameConsumer.consumeTextureFrame(texId,
                            AgoraVideoFrame.FORMAT_TEXTURE_2D, mWidth, mHeight,
                            0, System.currentTimeMillis(), mMatrix);
                }
            }
        });
        return true;
    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDispose() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                mRenderStopped = true;
            }
        });
    }

    public void release() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                mRenderStopped = true;
                GLES20.glDeleteTextures(1, new int[]{texId}, 0 );
                eglBase.release();
                eglBase = null;
                mEglContext = null;
            }
        });
    }

    @Override
    public int getBufferType() {
        return AgoraVideoFrame.BUFFER_TYPE_TEXTURE;
    }

    public EglBase.Context getEglContext() {
        return mEglContext;
    }

    private int createTexture() {
        if (mContext.get() == null) return 0;

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        texId = textures[0];
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.get().getResources(),
                R.drawable.tex_1);

        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        return texId;
    }
}
