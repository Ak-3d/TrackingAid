package com.ak.trackingaid;

import android.graphics.Bitmap;

import org.opencv.core.Scalar;

public class Variables {
    public volatile static Bitmap image;
    public volatile static boolean isCapturing;
    public volatile static Scalar lowerBounds;
    public volatile static Scalar upperBounds;
}
