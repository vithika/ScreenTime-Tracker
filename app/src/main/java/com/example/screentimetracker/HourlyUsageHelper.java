package com.example.screentimetracker;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.Calendar;

public class HourlyUsageHelper {

    public static long[] getHourlyUsage(Context context) {
        long[] hourlyMs = new long[24];

        try {
            UsageStatsManager usm =
                    (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

            // Start of today
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();
            long endTime   = System.currentTimeMillis();

            UsageEvents events = usm.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();

            String currentApp = null;
            long   sessionStart = 0;

            while (events.hasNextEvent()) {
                events.getNextEvent(event);

                String pkg  = event.getPackageName();
                long   ts   = event.getTimeStamp();
                int    type = event.getEventType();

                // Skip our own app
                if (pkg.equals(context.getPackageName())) continue;

                if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    currentApp   = pkg;
                    sessionStart = ts;

                } else if (type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (pkg.equals(currentApp) && sessionStart > 0) {
                        // Distribute session time across hours
                        distributeAcrossHours(hourlyMs, sessionStart, ts);
                        currentApp   = null;
                        sessionStart = 0;
                    }
                }
            }

            // Flush active session
            if (currentApp != null && sessionStart > 0) {
                distributeAcrossHours(hourlyMs, sessionStart, endTime);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return hourlyMs;
    }

    // Splits a session that spans multiple hours into each hour bucket
    private static void distributeAcrossHours(long[] hourlyMs, long start, long end) {
        if (end <= start) return;

        Calendar cal = Calendar.getInstance();

        long cursor = start;
        while (cursor < end) {
            cal.setTimeInMillis(cursor);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            // Find end of this hour slot
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long hourEnd = Math.min(cal.getTimeInMillis(), end);

            hourlyMs[hour] += (hourEnd - cursor);
            cursor = hourEnd + 1;
        }
    }
}
