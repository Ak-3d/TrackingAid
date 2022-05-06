package com.ak.trackingaid;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private ImageView img;
    private ImageView detectedColor;

    private SeekBar hlowerBar;
    private SeekBar hupperBar;

    private EditText hlowerText;
    private EditText hupperText;

    private View upperColor;
    private View lowerColor;

    private SeekBar satBar;
    private SeekBar valBar;

    private EditText satText;
    private EditText valText;

    private Scalar lowerBounds;
    private Scalar upperBounds;

    private Mat hcvImg;
    private Mat hcvDefault;
    private Mat threasholdImg;
    private Mat threasholdDefault;
    private Mat rgbImg;
    private Mat rgbDefault;

    private Bitmap loadedBtmp;

    private ActivityResultLauncher<String> loadingResultLanucher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        img = findViewById(R.id.img);
        detectedColor = findViewById(R.id.dedicte_colors);

        hupperBar   = findViewById(R.id.hupper_bar);
        hlowerBar = findViewById(R.id.hlower_bar);

        hupperText   =  findViewById(R.id.hupper_text);
        hlowerText =  findViewById(R.id.hlower_text);

        upperColor = findViewById(R.id.upper_color);
        lowerColor = findViewById(R.id.lower_color);


        satBar   = findViewById(R.id.sat_bar);
        valBar = findViewById(R.id.val_bar);

        satText   =  findViewById(R.id.sat_text);
        valText =  findViewById(R.id.val_text);

        initActions();
        initLoadingResult();
        initDefualt();
    }

    public void onLoad(View v){
        loadingResultLanucher.launch("image/*");
    }
    public void detect(){
        upperColor.setBackgroundColor(Color.HSVToColor(new float[]{2 * hupperBar.getProgress(), 1, 1}));/* 360 colors devided by 180 possibles*/
        lowerColor.setBackgroundColor(Color.HSVToColor(new float[]{2 * hlowerBar.getProgress(), 1, 1}));

        threasholdImg = new Mat();
        threasholdDefault = new Mat();
        Core.inRange(hcvImg, lowerBounds, upperBounds, threasholdImg);
        Core.inRange(hcvDefault, lowerBounds, upperBounds, threasholdDefault);

        Mat cloneRgb = new Mat();

        Core.bitwise_and(rgbImg, rgbImg, cloneRgb, threasholdImg);
        Bitmap rgbBtmp = Bitmap.createBitmap( cloneRgb.cols(), cloneRgb.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cloneRgb, rgbBtmp);
        img.setImageBitmap(rgbBtmp);

        cloneRgb = rgbDefault.clone();

        List<MatOfPoint> contours = new ArrayList<>();
        Mat her = new Mat();
        Imgproc.findContours(threasholdDefault, contours, her, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if(contours.size() > 0) {
            Bitmap rgbDefualtBtmp = Bitmap.createBitmap(cloneRgb.cols(), cloneRgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cloneRgb, rgbDefualtBtmp);
            Paint p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setColor(Color.RED);
            Canvas c = new Canvas(rgbDefualtBtmp);
            for(MatOfPoint m : contours) {
                if(Imgproc.contourArea(m) > 500){
                    Rect r = Imgproc.boundingRect(m);

                    c.drawRect(r.x, r.y, r.width + r.x, r.y + r.height, p);
                }
            }
            detectedColor.setImageBitmap(rgbDefualtBtmp);
            Log.d(TAG, "detect: detected");
        }
    }
    private void initDefualt(){
        //color to detect
        lowerBounds = Variables.lowerBounds;//new Scalar(widthColor * 4, 10, 10);
        upperBounds = Variables.upperBounds; //new Scalar(widthColor * 4 + widthColor, 255, 255);

        hupperBar.setProgress((int)upperBounds.val[0]);
        int[] val = new int[]{(int)lowerBounds.val[0], (int)lowerBounds.val[1], (int)lowerBounds.val[2]};
        hlowerBar.setProgress(val[0]);
        satBar.setProgress(val[1]);
        valBar.setProgress(val[2]);

        BitmapFactory.Options deduction = new BitmapFactory.Options();
        deduction.inSampleSize = 4;
        loadedBtmp = BitmapFactory.decodeResource(getResources(), R.mipmap.hue_val_sat,deduction);

        rgbImg = new Mat();
        rgbDefault = new Mat();

        Utils.bitmapToMat(loadedBtmp, rgbImg);
        rgbDefault = rgbImg.clone();

        hcvImg = new Mat();
        Imgproc.cvtColor(rgbImg, hcvImg, Imgproc.COLOR_RGB2HSV);
        hcvDefault = hcvImg.clone();

        Log.d(TAG, "loading complete address of Mat:" + rgbImg);
        detect();
    }

    private void initActions(){
        hlowerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hlowerText.setText(i + "");
                lowerBounds = new Scalar(i, satBar.getProgress(), valBar.getProgress());
                if(loadedBtmp != null)
                    detect();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        hupperBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hupperText.setText(i + "");
                upperBounds = new Scalar(i, 255, 255);
                if(loadedBtmp != null)
                    detect();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        satBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                satText.setText(i + "");
                lowerBounds = new Scalar(hlowerBar.getProgress(), i, valBar.getProgress());
                if(loadedBtmp != null)
                    detect();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        valBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                valText.setText(i + "");
                lowerBounds = new Scalar(hlowerBar.getProgress(), satBar.getProgress(), i);

                if(loadedBtmp != null)
                    detect();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        hupperText.setEnabled(false);
        hlowerText.setEnabled(false);
        satText.setEnabled(false);
        valText.setEnabled(false);
    }

    private void initLoadingResult(){
        loadingResultLanucher = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                if(result == null)return;

                ContentResolver cr = getContentResolver();
                InputStream in = null;
                try {
                    in = cr.openInputStream(result);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                loadedBtmp = BitmapFactory.decodeStream(in);
                img.setImageBitmap(loadedBtmp);

                rgbImg = new Mat();
                Utils.bitmapToMat(loadedBtmp, rgbImg);

                hcvImg = new Mat();
                Imgproc.cvtColor(rgbImg, hcvImg, Imgproc.COLOR_RGB2HSV);

                Log.d(TAG, "loading complete address of Mat:" + rgbImg);
                detect();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Variables.lowerBounds = lowerBounds;
        Variables.upperBounds = upperBounds;
    }

    public void onSubmit(View view) {
        finish();
    }
}