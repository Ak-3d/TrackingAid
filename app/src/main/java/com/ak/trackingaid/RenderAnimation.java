package com.ak.trackingaid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceHolder;

public class RenderAnimation implements Runnable{
    private static final String TAG = "RenderAnimation";

    private SurfaceHolder surfaceHolder;
    private int width;
    private int height;

    private Paint p;
    private RectF circle;

    public RenderAnimation(SurfaceHolder surfaceHolder, int width, int height) {
        this.surfaceHolder = surfaceHolder;
        this.width = width;
        this.height = height;

        p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.RED);

        circle = new RectF(0,0,100,100);
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
        if(circle.right < width - 1 && circle.top < 1)
            circle.offset(10, 0);
        else if(circle.right > width - 1 && circle.bottom < height - 1)
            circle.offset(0, 10);
        else if(circle.left > 1 && circle.bottom > height - 1)
            circle.offset(-10, 0);
        else
            circle.offset(0, -10);
    }
}
