package com.ak.trackingaid;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
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

    @SuppressLint("WrongConstant")//TODO search for better constant, ImageFormat.JPEG was not stable
    public void prepare() {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("screenshot", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
    }

    public void startCaptures() {
        capturing = new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    capture();
                    SystemClock.sleep(10);
                    Log.d(TAG, "run: capturing, interrupt:" + Thread.currentThread().isInterrupted());
                }
            }
        };
        capturing.start();
        Log.d(TAG, "startCaptures");
    }

    public void stopCaptures() {
        if(capturing != null){
            capturing.interrupt();
            capturing = null;
            Log.d(TAG, "stopCaptures");
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if(imageReader != null){
            imageReader.close();
            imageReader = null;
        }
    }
    public boolean isCapturing(){
        return capturing != null;
    }
    public void capture() {
        if (mediaProjection != null) {
            Bitmap bitmap = null;

            try (Image image = imageReader.acquireLatestImage()) {
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    Buffer imageBuffer = planes[0].getBuffer().rewind();

                    bitmap = Bitmap.createBitmap(width, height,
                            Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(imageBuffer);

                    imageProcessing(bitmap);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null)
                    bitmap.recycle();

            }
        }
    }

    public void imageProcessing(Bitmap bitmap) {
        if (bitmap != null) { //change this to local Variable
            Mat rgbImg = new Mat();
            Utils.bitmapToMat(bitmap, rgbImg);

            Mat hcvImg = new Mat();
            Imgproc.cvtColor(rgbImg, hcvImg, Imgproc.COLOR_RGB2HSV);

            Mat thresholdImg = new Mat();
            Core.inRange(hcvImg, Variables.lowerBounds, Variables.upperBounds, thresholdImg);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat her = new Mat();
            Imgproc.findContours(thresholdImg, contours, her, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            Rect r = null;
            if (contours.size() > 0) {
                for (MatOfPoint m : contours) {
                    if (Imgproc.contourArea(m) > 500) {
                        r = Imgproc.boundingRect(m);
                        break;
                    }
                }
            }
            send(r);
        }
    }

    private void send(Rect r) {
        if(r != null) {
            r.x = r.x + (r.width  -  width) / 2; // r.width  / 2 - width / 2;
            r.y = r.y + (r.height - height) / 2; // r.height / 2 - height / 2;
            Variables.x = r.x;
            Variables.y = r.y;
            builder.setContentText("position: " + r.x + ", " + r.y);
            managerCompat.notify(1, builder.build());

            Log.d(TAG, "send: " + r.x +", " + r.y);
        }
        else{
            Variables.x = 0;
            Variables.y = 0;
            builder.setContentText("position: " + 0 + ", " + 0); //don't move, assumes that the detected position is in the middle
            managerCompat.notify(1, builder.build());
        }
    }

    public class CaptureBinder extends Binder {
        public CaptureService getService() {
            return CaptureService.this;
        }
    }
}
