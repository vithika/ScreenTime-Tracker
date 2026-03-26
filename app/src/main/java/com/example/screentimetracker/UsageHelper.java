package com.example.screentimetracker;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UsageHelper {

    private static final long MAX_SESSION_MS = 4 * 60 * 60 * 1000;

    private static final Set<String> EXCLUDED_PACKAGES = new HashSet<>(Arrays.asList(
           // "com.actiondash.playstore",
            "com.google.android.apps.wellbeing",
            "com.samsung.android.forest",
            "com.oneplus.brickmode",
            "com.miui.securitycenter",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.samsung.android.app.launcher",
            "com.miui.home",
            "com.oneplus.launcher",
            "android",
            "com.android.settings",
            "com.example.screentimetracker"
    ));

    // ─── Permission Check ──────────────────────────────────────────────────────

    public static boolean hasUsagePermission(Context context) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, now - 1000, now);
        return stats != null && !stats.isEmpty();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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
            // Uninstalled but had usage — include it
            return true;
        }
    }

    private static String getAppName(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            String[] parts = pkg.split("\\.");
            if (parts.length > 0) {
                String name = parts[parts.length - 1];
                return Character.toUpperCase(name.charAt(0))
                        + name.substring(1)
                        + " (Uninstalled)";
            }
            return pkg + " (Uninstalled)";
        }
    }

    private static boolean isAppInstalled(Context context, String pkg) {
        try {
            context.getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static long[] getStartEndTime() {
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new long[]{calendar.getTimeInMillis(), endTime};
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public static long getTodayUsage(Context context) {
        if (!hasUsagePermission(context)) return -1L;

        List<AppUsageInfo> list = getAppUsageList(context);
        long total = 0;
        for (AppUsageInfo info : list) {
            total += info.usageTimeMs;
        }
        return total;
    }

    public static List<AppUsageInfo> getAppUsageList(Context context) {
        if (!hasUsagePermission(context)) return Collections.emptyList();

        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long[] range = getStartEndTime();
        long startTime = range[0];
        long endTime = range[1];

        // ✅ Always recalculate fresh from scratch — no accumulation
        Map<String, Long> freshMap = new HashMap<>();

        UsageEvents events = usm.queryEvents(startTime, endTime);
        String currentApp = null;
        long start = 0;

        UsageEvents.Event event = new UsageEvents.Event();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            String pkg = event.getPackageName();
            if (!shouldTrackApp(context, pkg)) continue;

            long ts = event.getTimeStamp();

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (currentApp != null) {
                    long time = ts - start;
                    if (time > 1000 && time < MAX_SESSION_MS) {
                        freshMap.merge(currentApp, time, Long::sum);
                    }
                }
                currentApp = pkg;
                start = Math.max(ts, startTime);

            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (pkg.equals(currentApp)) {
                    long time = ts - start;
                    if (time > 1000 && time < MAX_SESSION_MS) {
                        freshMap.merge(pkg, time, Long::sum);
                    }
                    currentApp = null;
                }
            }
        }

        // Flush active app
        if (currentApp != null) {
            long time = endTime - start;
            if (time > 1000 && time < MAX_SESSION_MS) {
                freshMap.merge(currentApp, time, Long::sum);
            }
        }

        // ✅ Load cache — only used to get uninstalled app data
        // Cache has correct times from when app was still installed
        Map<String, Long> cachedMap = UsageCache.loadUsage(context);

        // ✅ Merge: for installed apps use fresh live data
        // for uninstalled apps use cached data
        Map<String, Long> finalMap = new HashMap<>(cachedMap);
        for (Map.Entry<String, Long> entry : freshMap.entrySet()) {
            // Always overwrite with fresh data for installed apps
            finalMap.put(entry.getKey(), entry.getValue());
        }

        // ✅ Save fresh data back to cache
        // This keeps uninstalled app entries intact in cache
        // while updating installed app times correctly
        UsageCache.saveUsage(context, finalMap);

        // Convert to AppUsageInfo list
        List<AppUsageInfo> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : finalMap.entrySet()) {
            String pkg = entry.getKey();
            // Skip entries with 0 or negative time
            if (entry.getValue() <= 0) continue;
            result.add(new AppUsageInfo(
                    pkg,
                    getAppName(context, pkg),
                    entry.getValue(),
                    isAppInstalled(context, pkg),
                    CategoryHelper.getCategory(context, pkg)
            ));
        }

        result.sort((a, b) -> Long.compare(b.usageTimeMs, a.usageTimeMs));

        return result;
    }

    // ─── Debug ────────────────────────────────────────────────────────────────

    public static void debugUsageBreakdown(Context context) {
        List<AppUsageInfo> list = getAppUsageList(context);
        long total = 0;
        for (AppUsageInfo info : list) {
            long mins = info.usageTimeMs / 1000 / 60;
            long secs = (info.usageTimeMs / 1000) % 60;
            total += info.usageTimeMs;
            Log.d("UsageDebug", info.appName
                    + (info.isInstalled ? "" : " [UNINSTALLED]")
                    + " → " + mins + "m " + secs + "s");
        }
        Log.d("UsageDebug", "──────────────────────────────");
        Log.d("UsageDebug", "TOTAL → "
                + (total / 1000 / 60) + "m " + ((total / 1000) % 60) + "s");
    }

    // Add this method inside UsageHelper.java
    public static Drawable getAppIcon(Context context, String pkg) {
        try {
            return context.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            // App uninstalled — return default android icon
            return context.getPackageManager().getDefaultActivityIcon();
        }
    }
}