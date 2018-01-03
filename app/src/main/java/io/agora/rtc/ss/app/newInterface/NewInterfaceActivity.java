package io.agora.rtc.ss.app.newInterface;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.agora.rtc.ss.app.R;


public class NewInterfaceActivity extends Activity implements SurfaceReadyListener {
    private static final int RECORD_REQUEST_CODE = 101;
    private static final int STORAGE_REQUEST_CODE = 102;
    private static final int AUDIO_REQUEST_CODE = 103;
    private static int[] PICTURE_ARRAY = null;
    private static final String LOG_TAG = "MainActivity" + " tjy";
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;
    private Button startFullBtn;
    private LinearLayout viewLayout;
    private CheckBox enableViewBox;
    private ImageView recordView;
    private EditText channelName;
    private CheckBox enableLocal;
    private View localView;
    private int pictureCount = 0;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("TJY","onCreate：");
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        setContentView(R.layout.activity_new_interface);
        viewLayout = (LinearLayout)findViewById(R.id.view_record_layout);
        recordView = (ImageView) findViewById(R.id.recordView);
        startFullBtn = (Button) findViewById(R.id.start_full_record);
        enableViewBox = (CheckBox) findViewById(R.id.enable_view_record);
        channelName = (EditText) findViewById(R.id.channel_name);
        enableLocal = (CheckBox) findViewById(R.id.local_preview);
        PICTURE_ARRAY = new int[]{
                R.drawable.tex_0,
                R.drawable.tex_1,
                R.drawable.tex_2,
                R.drawable.tex_3};
        enableLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    showLocalView();
                } else {
                    removeLocalView();
                }
            }
        });

        startFullBtn.setEnabled(false);
        startFullBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordService.isRunning()) {
                    recordService.stopRecord();
                    startFullBtn.setText(R.string.start_full_record);
                } else {
                    if(channelName.getText().toString().length()<=0){
                        return;
                    }
                    Intent captureIntent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                }
            }
        });

        enableViewBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    //TODO  enable local preview
                    recordService.setEnableViewRecord(true);
                    viewLayout.setVisibility(View.VISIBLE);
                    recordService.setRecordView(recordView);
                } else {
                    //TODO  disable local preview
                    recordService.setEnableViewRecord(false);
                    viewLayout.setVisibility(View.GONE);
                }
            }
        });

        if (ContextCompat.checkSelfPermission(NewInterfaceActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(NewInterfaceActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }

        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
            Log.i("TJY","check permission ok");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            recordService.setChannelName(channelName.getText().toString());
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            Log.i("TJY", "init mediaProjection：" + mediaProjection + " recordView:" + recordView);
            recordService.setMediaProject(mediaProjection);
            recordService.startRecord();
            startFullBtn.setText(R.string.stop_full_record);
        }
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            recordService.setSurfaceReadyListener(NewInterfaceActivity.this);
            //recordService.initRtcEngine();
            startFullBtn.setEnabled(true);
            startFullBtn.setText(recordService.isRunning() ? R.string.stop_full_record : R.string.start_full_record);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void surfaceIsReady(View previewSurface) {
        Log.i(LOG_TAG, "set surfaceIsReady");
        localView = previewSurface;
    }

    public void showLocalView(){
        //先移除，再添加的套路
        if(localView!=null){
            removeLocalView();
            FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
            container.addView(localView);
            container.setVisibility(View.VISIBLE);
        }

    }

    public void removeLocalView() {
        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        if (container.getChildCount() > 0) {
            container.removeView(container.getChildAt(0));
        }
        container.setVisibility(View.INVISIBLE);
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    public void onChangePicture(View view) {
        int temp = (pictureCount % PICTURE_ARRAY.length);
        Log.i("TJY","temp："+temp);
        recordView.setImageResource(PICTURE_ARRAY[temp]);
        pictureCount++;
    }
}
