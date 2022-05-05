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

    private int thick;

    public RenderAnimation(SurfaceHolder surfaceHolder, int width, int height) {
        thick = 10;

        this.surfaceHolder = surfaceHolder;
        this.width = width - thick;
        this.height = height - thick;

        p = new Paint();
        p.setColor(Color.RED);
        p.setStrokeWidth(thick);

        circle = new RectF(thick,thick,100,100);
    }

    @Override
    public void run() {
        Log.d(TAG, "run: " + Thread.currentThread().isInterrupted());
        while(!Thread.currentThread().isInterrupted()){
            Canvas canvas = surfaceHolder.lockCanvas();
            if(canvas != null) {
                canvas.drawColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.BLACK);
                canvas.drawRect(thick, thick, width, height, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.RED);
                canvas.drawOval(circle, p);

                update();
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
    private void update(){
        if(circle.right < width - thick && circle.top < thick)
            circle.offset(10, 0);
        else if(circle.right > width - thick && circle.bottom < height - thick)
            circle.offset(0, 10);
        else if(circle.left > thick && circle.bottom > height - thick)
            circle.offset(-10, 0);
        else
            circle.offset(0, -10);
    }
}
