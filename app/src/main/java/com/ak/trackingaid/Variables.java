package com.ak.trackingaid;

import android.graphics.Bitmap;

import org.opencv.core.Scalar;

public class Variables { ///possible updating is using volatile keyword with the variable
    public static Bitmap image;
    public static Scalar lowerBounds;
    public static Scalar upperBounds;
    public static int x;
    public static int y;
}
