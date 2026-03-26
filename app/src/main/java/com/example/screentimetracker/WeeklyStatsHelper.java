package com.example.screentimetracker;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class WeeklyStatsHelper {

    private static final long MAX_SESSION_MS = 4 * 60 * 60 * 1000;

    private static final Set<String> EXCLUDED_PACKAGES = new HashSet<>(Arrays.asList(
            "com.actiondash.playstore",
            "com.google.android.apps.wellbeing",
            "com.samsung.android.forest",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.samsung.android.app.launcher",
            "com.miui.home",
            "com.oneplus.launcher",
            "android",
            "com.android.settings"
    ));

    private static boolean shouldTrackApp(Context context, String pkg) {
        if (pkg.equals(context.getPackageName())) return false;
        if (EXCLUDED_PACKAGES.contains(pkg)) return false;
        if (pkg.contains("launcher")) return false;

        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    || pm.getLaunchIntentForPackage(pkg) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * Returns array of 7 floats — usage in MINUTES for each day
     * Index 0 = 6 days ago, Index 6 = today
     */
    public static float[] getLast7DaysUsage(Context context) {
        float[] dailyUsage = new float[7];

        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // Calculate usage for each of the last 7 days
        for (int i = 6; i >= 0; i--) {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            start.add(Calendar.DAY_OF_YEAR, -i);

            Calendar end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_YEAR, 1);

            // For today don't go beyond current time
            if (i == 0) {
                end.setTimeInMillis(System.currentTimeMillis());
            }

            long totalMs = getDayUsage(context, usm,
                    start.getTimeInMillis(), end.getTimeInMillis());

            // Store in correct index — index 0 is 6 days ago, index 6 is today
            dailyUsage[6 - i] = totalMs / 1000f / 60f;
        }

        return dailyUsage;
    }

    private static long getDayUsage(Context context, UsageStatsManager usm,
                                    long startTime, long endTime) {
        UsageEvents events = usm.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();

        long totalMs = 0;
        String currentApp = null;
        long start = 0;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            String pkg = event.getPackageName();
            if (!shouldTrackApp(context, pkg)) continue;

            long ts = event.getTimeStamp();

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (currentApp != null) {
                    long time = ts - start;
                    if (time > 1000 && time < MAX_SESSION_MS) {
                        totalMs += time;
                    }
                }
                currentApp = pkg;
                start = Math.max(ts, startTime);

            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (pkg.equals(currentApp)) {
                    long time = ts - start;
                    if (time > 1000 && time < MAX_SESSION_MS) {
                        totalMs += time;
                    }
                    currentApp = null;
                }
            }
        }

        // Flush active app
        if (currentApp != null) {
            long time = endTime - start;
            if (time > 1000 && time < MAX_SESSION_MS) {
                totalMs += time;
            }
        }

        return totalMs;
    }

    /**
     * Returns day labels for x-axis — e.g. ["Mon", "Tue", ... "Today"]
     */
    public static String[] getDayLabels() {
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String[] labels = new String[7];

        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            if (i == 0) {
                labels[6] = "Today";
            } else {
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                labels[6 - i] = days[(dayOfWeek - i + 7 * 10) % 7];
            }
        }

        return labels;
    }
}