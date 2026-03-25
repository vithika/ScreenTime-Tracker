package com.example.screentimetracker.model;

import android.graphics.drawable.Drawable;

public class AppUsageModel {
    public final  String appName;
    public final  long time;
    public final  Drawable icon;
    public final  boolean isInstalled;


    public AppUsageModel(String appName, long time, Drawable icon, boolean isInstalled) {
        this.appName = appName;
        this.time = time;
        this.icon = icon;
        this.isInstalled = isInstalled;
    }

    public String getAppName() { return appName; }
    public long getTime() { return time; }
}

