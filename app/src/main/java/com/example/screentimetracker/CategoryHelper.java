package com.example.screentimetracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CategoryHelper {

    // ─── Known app package → category mapping ────────────────────────────────
    // Fallback for when Play Store category is unavailable
    private static final Map<String, String> KNOWN_APPS = new HashMap<String, String>() {{
        // Social
        put("com.whatsapp",                AppCategory.SOCIAL);
        put("com.whatsapp.w4b",            AppCategory.SOCIAL);
        put("com.instagram.android",       AppCategory.SOCIAL);
        put("com.facebook.katana",         AppCategory.SOCIAL);
        put("com.facebook.lite",           AppCategory.SOCIAL);
        put("com.twitter.android",         AppCategory.SOCIAL);
        put("com.snapchat.android",        AppCategory.SOCIAL);
        put("com.linkedin.android",        AppCategory.SOCIAL);
        put("com.pinterest",               AppCategory.SOCIAL);
        put("com.reddit.frontpage",        AppCategory.SOCIAL);
        put("org.telegram.messenger",      AppCategory.SOCIAL);
        put("com.discord",                 AppCategory.SOCIAL);
        put("com.skype.raider",            AppCategory.SOCIAL);

        // Entertainment
        put("com.google.android.youtube",  AppCategory.ENTERTAINMENT);
        put("com.netflix.mediaclient",     AppCategory.ENTERTAINMENT);
        put("com.amazon.avod.thirdpartyclient", AppCategory.ENTERTAINMENT);
        put("com.hotstar.android",         AppCategory.ENTERTAINMENT);
        put("com.jio.media.ondemand",      AppCategory.ENTERTAINMENT);
        put("com.sony.liv.tv",             AppCategory.ENTERTAINMENT);
        put("com.zee5.android",            AppCategory.ENTERTAINMENT);
        put("in.startv.hotstar",           AppCategory.ENTERTAINMENT);
        put("com.mxtech.videoplayer.ad",   AppCategory.ENTERTAINMENT);
        put("com.spotify.music",           AppCategory.ENTERTAINMENT);
        put("com.gaana",                   AppCategory.ENTERTAINMENT);
        put("com.jio.music",               AppCategory.ENTERTAINMENT);
        put("com.apple.android.music",     AppCategory.ENTERTAINMENT);
        put("com.twitch.android.app",      AppCategory.ENTERTAINMENT);

        // Education
        put("com.duolingo",                AppCategory.EDUCATION);
        put("com.byju.learning",           AppCategory.EDUCATION);
        put("org.khanacademy.android",     AppCategory.EDUCATION);
        put("com.google.android.apps.classroom", AppCategory.EDUCATION);
        put("com.unacademy",               AppCategory.EDUCATION);
        put("com.vedantu.student",         AppCategory.EDUCATION);
        put("com.coursera.android",        AppCategory.EDUCATION);
        put("com.udemy.android",           AppCategory.EDUCATION);

        // Food
        put("in.swiggy.android",           AppCategory.FOOD);
        put("com.application.zomato",      AppCategory.FOOD);
        put("com.ubercab.eats",            AppCategory.FOOD);
        put("com.mcdonalds.mobileapp",     AppCategory.FOOD);

        // Productivity
        put("com.google.android.gm",       AppCategory.PRODUCTIVITY);
        put("com.microsoft.office.outlook",AppCategory.PRODUCTIVITY);
        put("com.google.android.apps.docs",AppCategory.PRODUCTIVITY);
        put("com.google.android.apps.sheets", AppCategory.PRODUCTIVITY);
        put("com.google.android.apps.slides", AppCategory.PRODUCTIVITY);
        put("com.microsoft.office.word",   AppCategory.PRODUCTIVITY);
        put("com.microsoft.office.excel",  AppCategory.PRODUCTIVITY);
        put("com.notion.id",               AppCategory.PRODUCTIVITY);
        put("com.todoist",                 AppCategory.PRODUCTIVITY);
        put("com.slack",                   AppCategory.PRODUCTIVITY);
        put("com.microsoft.teams",         AppCategory.PRODUCTIVITY);
        put("us.zoom.videomeetings",       AppCategory.PRODUCTIVITY);
        put("com.google.android.apps.meetings", AppCategory.PRODUCTIVITY);

        // Finance
        put("com.phonepe.app",             AppCategory.FINANCE);
        put("net.one97.paytm",             AppCategory.FINANCE);
        put("com.google.android.apps.nbu.paisa.user", AppCategory.FINANCE);
        put("com.amazon.mShop.android.shopping", AppCategory.FINANCE);
        put("com.robinhood.android",       AppCategory.FINANCE);

        // Health
        put("com.google.android.apps.fitness", AppCategory.HEALTH);
        put("com.samsung.android.shealth", AppCategory.HEALTH);
        put("com.nike.plusgps",            AppCategory.HEALTH);
        put("com.strava",                  AppCategory.HEALTH);

        // Shopping
        put("com.amazon.mShop.android",    AppCategory.SHOPPING);
        put("com.flipkart.android",        AppCategory.SHOPPING);
        put("com.myntra.android",          AppCategory.SHOPPING);
        put("com.meesho.supply",           AppCategory.SHOPPING);

        // Travel
        put("com.makemytrip",              AppCategory.TRAVEL);
        put("com.booking",                 AppCategory.TRAVEL);
        put("com.airbnb.android",          AppCategory.TRAVEL);
        put("com.ubercab",                 AppCategory.TRAVEL);
        put("com.olacabs.customer",        AppCategory.TRAVEL);
        put("com.rapido.passenger",        AppCategory.TRAVEL);

        // News
        put("com.google.android.apps.magazines", AppCategory.NEWS);
        put("com.inshorts.newsbucket",     AppCategory.NEWS);
        put("com.eterno",                  AppCategory.NEWS);
        put("com.ndtv.news",               AppCategory.NEWS);

        // Games
        put("com.activision.callofduty.shooter", AppCategory.GAMES);
        put("com.pubg.imobile",            AppCategory.GAMES);
        put("com.garena.free.fire",        AppCategory.GAMES);
        put("com.supercell.clashofclans",  AppCategory.GAMES);
        put("com.king.candycrushsaga",     AppCategory.GAMES);
    }};

    // ─── Android API 26+ category mapping ────────────────────────────────────
    private static String getCategoryFromApi(int category) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return null;
        }
        switch (category) {
            case ApplicationInfo.CATEGORY_SOCIAL:        return AppCategory.SOCIAL;
            case ApplicationInfo.CATEGORY_VIDEO:         return AppCategory.ENTERTAINMENT;
            case ApplicationInfo.CATEGORY_AUDIO:         return AppCategory.ENTERTAINMENT;
            case ApplicationInfo.CATEGORY_IMAGE:         return AppCategory.ENTERTAINMENT;
            case ApplicationInfo.CATEGORY_GAME:          return AppCategory.GAMES;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:  return AppCategory.PRODUCTIVITY;
            case ApplicationInfo.CATEGORY_NEWS:          return AppCategory.NEWS;
            case ApplicationInfo.CATEGORY_MAPS:          return AppCategory.TRAVEL;
            default:                                     return null;
        }
    }

    /**
     * Returns category for a given package name.
     * Priority: Known apps map → Android API category → OTHER
     */
    public static String getCategory(Context context, String pkg) {
        // 1. Check known apps map first
        if (KNOWN_APPS.containsKey(pkg)) {
            return KNOWN_APPS.get(pkg);
        }

        // 2. Try Android API category (API 26+)
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(pkg, 0);
            String apiCategory = getCategoryFromApi(ai.category);

            if (apiCategory != null) {
                return apiCategory;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // App uninstalled — fall through to OTHER
        }

        // 3. Default
        return AppCategory.OTHER;
    }

    /**
     * Groups usage list by category
     * Returns map of category → total minutes
     */
    public static Map<String, Long> groupByCategory(Context context,
                                                    java.util.List<AppUsageInfo> appList) {
        Map<String, Long> categoryMap = new HashMap<>();
        for (AppUsageInfo info : appList) {
            String category = getCategory(context, info.packageName);

            long mins = info.usageTimeMs / 1000 / 60;
            if (categoryMap.containsKey(category)) {
                categoryMap.put(category, categoryMap.get(category) + mins);
            } else {
                categoryMap.put(category, mins);
            }
        }
        return categoryMap;
    }
}