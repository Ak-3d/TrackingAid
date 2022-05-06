package com.ak.trackingaid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;

public class RenderAnimation implements Runnable, SurfaceHolder.Callback, View.OnTouchListener {
    private static final String TAG = "RenderAnimation";

    private final SurfaceHolder surfaceHolder;
    private int width;
    private int height;

    private final Paint p;
    private final RectF circle;

    private final int speed;
    private final int rad;

    public RenderAnimation(SurfaceHolder surfaceHolder) {
        speed = 2;
        rad = 40;

        this.surfaceHolder = surfaceHolder;

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
                canvas.drawColor(Color.WHITE);

                canvas.drawOval(circle, p);

                update();
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
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

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        SystemClock.sleep(10);
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                circle.offsetTo(motionEvent.getX() - rad, motionEvent.getY() - rad);
                break;
            case MotionEvent.ACTION_MOVE:
                circle.offsetTo(motionEvent.getX() - rad, motionEvent.getY() - rad);
                break;
            case MotionEvent.ACTION_UP:
                circle.offsetTo(motionEvent.getX() - rad, motionEvent.getY() - rad);
                break;
        }
        return true;
    }
}
