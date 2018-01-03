package io.agora.rtc.ss.app.newInterface.source;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;

import io.agora.rtc.mediaio.IVideoFrameConsumer;
import io.agora.rtc.mediaio.IVideoSource;
import io.agora.rtc.video.AgoraVideoFrame;

public class ViewSharingCapturer implements IVideoSource {
    private static final int FPS = 10;
    private IVideoFrameConsumer mIVideoFrameConsumer;
    private boolean mCapturerIsPaused=false;
    private ViewCaptureSetting mViewCapturerSettings;
    private View mContentView;
    private View mTtestView;
    private Handler mFrameProducerHandler;
    private long mFrameProducerIntervalMillis = 1000 / FPS;
    private Runnable mFrameProducer = new Runnable() {
        @Override
        public void run() {
            if(mContentView!=null){
                Log.i("TJY","setDrawingCacheEnabled");
                mContentView.setDrawingCacheEnabled(true);
                mContentView.buildDrawingCache();
                //这里是对bitmap的处理
                Bitmap bmp = mContentView.getDrawingCache();

                byte[] byteTemp = getRGBFromBMP(bmp);
                //ByteBuffer buf = ByteBuffer.allocate(bmp.getByteCount());
                //bmp.copyPixelsToBuffer(buf);
/*                if(bmp != null){
                    ((ImageView)mTtestView).setImageBitmap(bmp);
                    //saveBitmap(bmp);
                }*/
                //mContentView.setDrawingCacheEnabled(false);

                if (bmp != null) {

                    Log.i("TJY","try to push rgba......");
                    mIVideoFrameConsumer.consumeByteArrayFrame(
                            byteTemp
                            , AgoraVideoFrame.FORMAT_RGBA, bmp.getWidth(), bmp.getHeight(), 0, System.currentTimeMillis());
                    Log.i("TJY","try to push rgba over.....");
                }
                mContentView.setDrawingCacheEnabled(false);
                if (!mCapturerIsPaused) {
                    Log.i("TJY","try to postDelayed rgba again......");
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



    public void setTestView(View view)
    {
        Log.i("TJY","setTestView");
        this.mTtestView = view;
    }


    public boolean onInitialize(IVideoFrameConsumer var1){
        try {
            Log.i("TJY","onInitialize");
            this.mIVideoFrameConsumer = var1;
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }
        return true;
    }

    public boolean onStart(){
        try {
            mCapturerIsPaused = false;
            Log.i("TJY","try to onStart mFrameProducerIntervalMillis");
            mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
        return true;
    }

    public void onStop(){
        Log.i("TJY","ViewSharingCapturer onStop");
        mCapturerIsPaused = true;
        mFrameProducerHandler.removeCallbacks(mFrameProducer);
    }

    public void onDispose(){
        Log.i("TJY","ViewSharingCapturer onDispose");
        mCapturerIsPaused = true;
        mFrameProducerHandler.removeCallbacks(mFrameProducer);
    }

    public int getBufferType(){
        return mViewCapturerSettings.getFormat();
    }

/*    public void saveBitmap(Bitmap bm) {
        Log.i("TJY", "保存图片");
        File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis()+".PNG");
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Log.i("TJY", "已经保存 :"+ Environment.getExternalStorageDirectory());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }*/


    public byte[] getPixelsBGRA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

        byte[] temp = buffer.array(); // Get the underlying array containing the data.

        byte[] pixels = new byte[temp.length]; // Allocate for BGRA

        // Copy pixels into place
        for (int i = 0; i < (temp.length / 4); i++) {

            pixels[i * 4] = temp[i * 4 ];        //r    B
            pixels[i * 4 + 1] = temp[i * 4 + 1];    //g    G
            pixels[i * 4 + 2] =  temp[i * 4 + 2];      //b    R
            pixels[i * 4 + 3] = temp[i * 4 + 3];    //a    A
        }

        return pixels;
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
                pixels[k * 4+ 3] = (byte) Color.red(color);
                pixels[k * 4 + 2] = (byte) Color.green(color);
                pixels[k * 4 + 1] = (byte) Color.blue(color);
                pixels[k * 4 + 0] = (byte) Color.alpha(color);
                k++;
            }
        }

        return pixels;
    }


    public static int shortToByteArray1(short i, byte[] data, int offset) {
        data[offset + 1] = (byte) (i >> 8 & 255);
        data[offset] = (byte) (i & 255);
        return offset + 2;
    }

    public static int RGB888ToRGB565(int rgb8888) {
        return (rgb8888 >> 19 & 31) << 11 | (rgb8888 >> 10 & 63) << 5 | rgb8888 >> 3 & 31;
    }

}