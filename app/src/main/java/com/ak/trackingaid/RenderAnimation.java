package com.ak.trackingaid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class RenderAnimation implements Runnable, SurfaceHolder.Callback, View.OnTouchListener {
    private static final String TAG = "RenderAnimation";

    private final SurfaceHolder surfaceHolder;
    private final TextView posTxtv;
    private final Context c;

    private int width;
    private int height;

    private final Paint p;
    private final RectF circle;

    private final int speed;
    private final int rad;

    public RenderAnimation(Context c, SurfaceHolder surfaceHolder, TextView posTxtv) {
        speed = 2;
        rad = 40;

        this.surfaceHolder = surfaceHolder;
        this.posTxtv = posTxtv;
        this.c = c;

        p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.RED);

        circle = new RectF(0, 0,rad * 2,rad * 2);
        surfaceHolder.addCallback(this);
    }

    @Override
    public void run() {
        Log.d(TAG, "run: " + Thread.currentThread().isInterrupted());
        while(!Thread.currentThread().isInterrupted()){
            Canvas canvas = surfaceHolder.lockCanvas();
            if(canvas != null) {
                updateView();

                canvas.drawColor(Color.WHITE);

                canvas.drawOval(circle, p);

                update();
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
    private void updateView(){
        posTxtv.post(() ->{
            String posStr = c.getString(R.string.position) + Variables.x + ", " + Variables.y;
            posTxtv.setText(posStr);
        });
    }
    private void update(){
        if(circle.right < width - rad && circle.top <= rad) //going right
            circle.offset(speed, 0);

        else if(circle.right >= width - rad && circle.bottom < height - rad) //going down
            circle.offset(0, speed);

        else if(circle.left > rad && circle.bottom >= height - rad) //going left
            circle.offset(-speed, 0);

        else if(circle.left <= rad && circle.top > rad) //going up
            circle.offset(0, -speed);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated: ");
        Canvas c = surfaceHolder.lockCanvas();
        c.drawColor(Color.WHITE);
        surfaceHolder.unlockCanvasAndPost(c);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged: ");
        width = i1;
        height = i2;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed: ");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        SystemClock.sleep(10);
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                circle.offsetTo(motionEvent.getX() - rad, motionEvent.getY() - rad);

        }
        return true;
    }
}
