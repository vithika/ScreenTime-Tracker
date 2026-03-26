package com.example.screentimetracker.model;

import android.graphics.drawable.Drawable;

public class AppUsageModel {
    public final  String appName;
    public final  long time;
    public final  Drawable icon;
    public final  boolean isInstalled;

    public  final String category;


    public AppUsageModel(String appName, long time, Drawable icon, boolean isInstalled,String category) {
        this.appName = appName;
        this.time = time;
        this.icon = icon;
        this.isInstalled = isInstalled;
        this.category = category;


    }

    public String getAppName() { return appName; }
    public long getTime() { return time; }
}

