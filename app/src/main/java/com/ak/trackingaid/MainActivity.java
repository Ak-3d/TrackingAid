package com.ak.trackingaid;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Scalar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    static{
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "Opencv Initialized sucssufuly.");
        }else{
            Log.d(TAG, "Opencv was not loaded.");
        }
    }
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private Button captureBtn;

    private ImageView imageView;

    private CaptureService captureService;
    private Thread viewUpdate;

    private ActivityResultLauncher<Intent> capture_launcher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        setContentView(R.layout.activity_main);

        captureBtn = findViewById(R.id.capture_btn);
        imageView = findViewById(R.id.image);

        Variables.isCapturing = false;
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(captureService != null) {
            captureService.stopCaptures();
            captureBtn.setText("start");
            viewUpdate = null;
        }
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }

    private void init(){

        Toolbar t = findViewById(R.id.toolbar);
        setSupportActionBar(t);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Variables.lowerBounds = new Scalar(120, 106, 106);
        Variables.upperBounds = new Scalar(142, 255, 255);
        captureBtn.setOnClickListener((view -> {
            if(mediaProjection == null){
                capture_launcher.launch(projectionManager.createScreenCaptureIntent());
                return;
            }
            if(Variables.isCapturing){
                captureService.stopCaptures();
                captureBtn.setText("start");
                viewUpdate = null;
            }else{
                captureService.prepare();
                captureService.startCaptures();
                captureBtn.setText("stop");
                createViewThread();
                viewUpdate.start();
            }

        }));
        createLaunchers();
    }
    private void createViewThread(){
        viewUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                while (Variables.isCapturing){
                    if(Variables.image != null)
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(Variables.image);
                            }
                        });

                    SystemClock.sleep(10);
                }
            }
        });
    }
    private void createLaunchers(){
        capture_launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                mediaProjection = projectionManager.getMediaProjection(result.getResultCode(), result.getData());
                if(mediaProjection != null) {
                    Intent i = new Intent(MainActivity.this, CaptureService.class);
                    bindService(i, connection, BIND_AUTO_CREATE);
                }
            }
        });
        capture_launcher.launch(projectionManager.createScreenCaptureIntent());
    }
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CaptureService.CaptureBinder binder =(CaptureService.CaptureBinder) iBinder;
            captureService = binder.getService();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            captureService.config(mediaProjection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            captureService.setupNotification();
            captureService.prepare();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}