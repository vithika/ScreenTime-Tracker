package com.example.screentimetracker.model;

public class AppUsageModel {
    String appName;
    long time;

    public AppUsageModel(String appName, long time) {
        this.appName = appName;
        this.time = time;
    }

    public String getAppName() { return appName; }
    public long getTime() { return time; }
}

