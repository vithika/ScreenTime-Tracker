package com.example.screentimetracker;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public static float[] getLastWeekUsage(Context context) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar calendar = Calendar.getInstance();

        // End = start of this week (last Monday midnight)
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long endTime = calendar.getTimeInMillis();

        // Start = 7 days before end
        long startTime = endTime - 7L * 24 * 60 * 60 * 1000;

        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        Map<Integer, Long> dailyMap = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        if (stats != null) {
            for (UsageStats stat : stats) {
                cal.setTimeInMillis(stat.getFirstTimeStamp());
                int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
                long prev = dailyMap.containsKey(dayOfYear)
                        ? dailyMap.get(dayOfYear) : 0L;
                dailyMap.put(dayOfYear, prev + stat.getTotalTimeInForeground());
            }
        }

        // Build 7-day array starting from last Monday
        float[] result = new float[7];
        Calendar dayCal = Calendar.getInstance();
        dayCal.setTimeInMillis(startTime);

        for (int i = 0; i < 7; i++) {
            int dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR);
            long ms = dailyMap.containsKey(dayOfYear) ? dailyMap.get(dayOfYear) : 0L;
            result[i] = ms / (1000f * 60f);
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return result;
    }
    public static float getWeeklyAverage(float[] data) {
        float sum   = 0;
        int   count = 0;
        for (float val : data) {
            if (val > 0) {
                sum += val;
                count++;
            }
        }
        return count == 0 ? 0f : sum / count;
    }

    // Format minutes → "Xh Ym" or "Ym"
    public static String formatMinutes(float minutes) {
        int total = (int) minutes;
        int h     = total / 60;
        int m     = total % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }


}