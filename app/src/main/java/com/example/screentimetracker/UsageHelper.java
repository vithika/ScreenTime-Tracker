package com.example.screentimetracker;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UsageHelper {

    private static final long MAX_SESSION_MS = 4 * 60 * 60 * 1000; // 4 hour cap per session

    // ─── Permission Check ────────────────────────────────────────────────────────

    public static boolean hasUsagePermission(Context context) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, now - 1000, now);
        return stats != null && !stats.isEmpty();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static boolean shouldTrackApp(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            // Include user-installed apps + system apps that have a launcher
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    || pm.getLaunchIntentForPackage(pkg) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * Returns [startOfDayMillis, nowMillis]
     */
    private static long[] getStartEndTime() {
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new long[]{calendar.getTimeInMillis(), endTime};
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    /**
     * Returns total screen time in milliseconds for today.
     * Returns -1 if usage permission is not granted.
     */
    public static long getTodayUsage(Context context) {
        if (!hasUsagePermission(context)) return -1L;

        Map<String, Long> usageMap = getAppUsageList(context);

        long total = 0;
        for (long time : usageMap.values()) {
            total += time;
        }
        return total;
    }

    /**
     * Returns a map of packageName -> usage time in milliseconds for today.
     * Returns empty map if usage permission is not granted.
     */
    public static Map<String, Long> getAppUsageList(Context context) {
        if (!hasUsagePermission(context)) return Collections.emptyMap();

        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long[] range = getStartEndTime();
        long startTime = range[0]; // midnight
        long endTime = range[1];   // now

        UsageEvents events = usm.queryEvents(startTime, endTime);

        Map<String, Long> usageMap = new HashMap<>();
        String currentApp = null;
        long start = 0;

        UsageEvents.Event event = new UsageEvents.Event();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            String pkg = event.getPackageName();
            if (!shouldTrackApp(context, pkg)) continue;

            long ts = event.getTimeStamp();

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {

                // Flush previous app if no BACKGROUND event arrived (app switch)
                if (currentApp != null) {
                    long time = ts - start;
                    if (time > 1000 && time < MAX_SESSION_MS) {
                        usageMap.merge(currentApp, time, Long::sum);
                    }
                }

                currentApp = pkg;
                // Clamp start to midnight to avoid stale events from yesterday
                start = Math.max(ts, startTime);

            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {

                if (pkg.equals(currentApp)) {
                    long time = ts - start;
                    if (time > 1000 && time < MAX_SESSION_MS) {
                        usageMap.merge(pkg, time, Long::sum);
                    }
                    currentApp = null;
                }
            }
        }

        // App is still in foreground at query time — flush it
        if (currentApp != null) {
            long time = endTime - start;
            if (time > 1000 && time < MAX_SESSION_MS) {
                usageMap.merge(currentApp, time, Long::sum);
            }
        }

        return usageMap;
    }






}
