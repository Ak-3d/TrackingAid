package com.ak.trackingaid;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class CaptureService extends Service {

    private static final String TAG = "CaptureService";

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private Thread capturing;

    private int width;
    private int height;
    private int dpi;

    private NotificationManagerCompat managerCompat;
    private NotificationCompat.Builder builder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new CaptureBinder();
    }

    public void setupNotification() {
        managerCompat = NotificationManagerCompat.from(this);
        builder = new NotificationCompat.Builder(this, App.MAIN_CHANNEL)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle("Capturing")
                .setSmallIcon(R.drawable.pic_note)
                .setContentText("position: ");
        managerCompat.notify(1, builder.build());
    }

    public void config(MediaProjection mediaProjection, int width, int height, int dpi) {
        this.mediaProjection = mediaProjection;
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public void prepare() {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("screenshot", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
    }

    public void startCaptures() {
        Variables.isCapturing = true;
        capturing = new Thread() {
            @Override
            public void run() {
                while (Variables.isCapturing) {
                    try {
                        capture();
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        capturing.start();
    }

    public void stopCaptures() {
        if (virtualDisplay == null || capturing == null) {
            return;
        }
        Variables.isCapturing = false;
        try {
            capturing.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        virtualDisplay.release();
        virtualDisplay = null;
        imageReader.close();
        imageReader = null;
        capturing = null;
    }

    public void capture() {
        if (mediaProjection != null) {
            Image image = null;
            Bitmap bitmap = null;
            try {
                image = imageReader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    Buffer imageBuffer = planes[0].getBuffer().rewind();

                    int width = image.getWidth();
                    int height = image.getHeight();

                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;

                    bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                            Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(imageBuffer);

                    imageProcessing(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null)
                    bitmap.recycle();

                if (image != null)
                    image.close();
            }
        }
    }

    public void imageProcessing(Bitmap bitmap) {
        if (bitmap != null) { //change this to local Variable
            Mat rgbImg = new Mat();
            Utils.bitmapToMat(bitmap, rgbImg);

            Mat hcvImg = new Mat();
            Imgproc.cvtColor(rgbImg, hcvImg, Imgproc.COLOR_RGB2HSV);

            Mat threasholdImg = new Mat();
            Core.inRange(hcvImg, Variables.lowerBounds, Variables.upperBounds, threasholdImg);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat her = new Mat();
            Imgproc.findContours(threasholdImg, contours, her, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            if (contours.size() > 0) {
                Bitmap temp = Bitmap.createBitmap(rgbImg.cols(), rgbImg.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgbImg, temp);

                Canvas c = new Canvas(temp);
                Paint p = new Paint();
                p.setStyle(Paint.Style.FILL);
                p.setStrokeWidth(5);
                p.setColor(Color.argb(100, 100, 0, 0));

                Rect r = null;
                for (MatOfPoint m : contours) {
                    if (Imgproc.contourArea(m) > 500) {
                        r = Imgproc.boundingRect(m);
                        c.drawRect(r.x, r.y, r.x + r.width, r.y + r.height, p);
                        break;
                    }
                }
                Variables.image = temp;

                send(r);
                Log.d(TAG, "detect: detected");
            }
        }
    }

    private void send(Rect r) {
        if(r != null) {
            r.x -= (width  + r.width ) / 2;
            r.y -= (height + r.height) / 2;
            builder.setContentText("position: " + r.x + ", " + r.y);
            managerCompat.notify(1, builder.build());
        }
    }

    public class CaptureBinder extends Binder {
        public CaptureService getService() {
            return CaptureService.this;
        }
    }
}
