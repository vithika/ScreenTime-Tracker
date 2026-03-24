package com.example.screentimetracker;

public class AppUsageInfo {
    public final String packageName;
    public final String appName;
    public final long usageTimeMs;
    public final boolean isInstalled;

    public AppUsageInfo(String packageName, String appName,
                        long usageTimeMs, boolean isInstalled) {
        this.packageName = packageName;
        this.appName = appName;
        this.usageTimeMs = usageTimeMs;
        this.isInstalled = isInstalled;
    }
}