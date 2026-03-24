package com.example.screentimetracker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UsageCache {

    private static final String PREF_NAME = "usage_cache";
    private static final String KEY_DATE  = "cache_date";
    private static final String KEY_USAGE = "cache_usage";

    // Save usage map — called once after full recalculation
    public static void saveUsage(Context context, Map<String, Long> usageMap) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // ✅ Clear old cache if it's a new day
        String cachedDate = prefs.getString(KEY_DATE, "");
        SharedPreferences.Editor editor = prefs.edit();

        if (!cachedDate.equals(getTodayKey())) {
            editor.clear();
        }

        editor.putString(KEY_DATE, getTodayKey());

        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, Long> entry : usageMap.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            editor.putString(KEY_USAGE, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        editor.apply();
    }

    // Load cached usage — returns empty map if cache is from a different day
    public static Map<String, Long> loadUsage(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String cachedDate = prefs.getString(KEY_DATE, "");
        if (!cachedDate.equals(getTodayKey())) {
            return new HashMap<>();
        }

        Map<String, Long> usageMap = new HashMap<>();
        String jsonStr = prefs.getString(KEY_USAGE, "{}");

        try {
            JSONObject json = new JSONObject(jsonStr);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String pkg = keys.next();
                usageMap.put(pkg, json.getLong(pkg));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return usageMap;
    }

    public static void clearCache(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    public static String getTodayKey() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-"
                + cal.get(Calendar.MONTH) + "-"
                + cal.get(Calendar.DAY_OF_MONTH);
    }
}