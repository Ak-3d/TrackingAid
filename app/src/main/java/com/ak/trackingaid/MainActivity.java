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
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

    private TextView positionTxt;

    private SurfaceView animation_view;
    private Thread renderAnimationThrd;

    private CaptureService captureService;
    private Thread viewUpdateThrd;

    private ActivityResultLauncher<Intent> capture_launcher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        setContentView(R.layout.activity_main);

        captureBtn = findViewById(R.id.capture_btn);
        imageView = findViewById(R.id.image);
        positionTxt = findViewById(R.id.position_main_view);
        animation_view = findViewById(R.id.animation_view);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            interruptThreads();
        }
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }

    private void init(){
        Toolbar t = findViewById(R.id.toolbar);
        setSupportActionBar(t);

        Variables.lowerBounds = new Scalar(120, 106, 106);
        Variables.upperBounds = new Scalar(142, 255, 255);

        Variables.x = 0;
        Variables.y = 0;

        captureBtn.setOnClickListener((view -> {
            if(mediaProjection == null){
                capture_launcher.launch(projectionManager.createScreenCaptureIntent());
                return;
            }
            if(captureService.isCapturing()){
                interruptThreads();
                captureService.stopCaptures();
                captureBtn.setText("start");
            }else{
                captureService.prepare();
                captureService.startCaptures();
                captureBtn.setText("stop");
                createViewThreads();
            }

        }));
        createLaunchers();
    }
    private void createViewThreads(){
        viewUpdateThrd = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()){
                if(Variables.image != null)
                    imageView.post(() -> {
                        imageView.setImageBitmap(Variables.image);
                        positionTxt.setText(getString(R.string.position) + Variables.x + ", " + Variables.y);
                    });

                Log.d(TAG, "createViewThreads: running");
                SystemClock.sleep(10);
            }
        });
        viewUpdateThrd.start();

        renderAnimationThrd = new Thread(new RenderAnimation(animation_view.getHolder(), animation_view.getWidth(), animation_view.getHeight()));
        renderAnimationThrd.start();

        Log.d(TAG, "createViewThreads: ThreadsCreated");
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

    @Override
    protected void onPause() {
        super.onPause();
        interruptThreads();
    }

    private void interruptThreads(){
        if(renderAnimationThrd != null) {
            renderAnimationThrd.interrupt();
            renderAnimationThrd = null;
        }
        if(viewUpdateThrd != null) {
            viewUpdateThrd.interrupt();
            viewUpdateThrd = null;
           Log.d(TAG, "onPause: threads are interrupted");
        }

        //I excluded stopCapturing because this function is used in onPause
    }
}