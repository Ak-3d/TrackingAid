package com.ak.trackingaid;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class App extends Application {

    private static final String TAG = "App";
    public static final String MAIN_CHANNEL = "MainChannel";

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(
                    MAIN_CHANNEL,
                    "mainChannel",
                    NotificationManager.IMPORTANCE_LOW
            ));

            System.out.println("this is very un-useful");

            //TODO is logging takes time
            Log.d(TAG, "onCreate: channeles are created");
        }
    }
}
