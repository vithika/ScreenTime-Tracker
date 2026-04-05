//package com.example.screentimetracker
//
//import android.content.Context
//
//object StreakHelper {
//
//    private const val PREFS = "streak_prefs"
//    private const val KEY_STREAK = "current_streak"
//    private const val KEY_LAST_DATE = "last_checked_date"
//    private const val KEY_BEST_STREAK = "best_streak"
//
//    fun updateStreak(context: Context, totalMinutes: Long, goalMinutes: Int) {
//        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//        val today = getTodayString()
//        val lastDate = prefs.getString(KEY_LAST_DATE, "")
//        var currentStreak = prefs.getInt(KEY_STREAK, 0)
//        var bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)
//
//        val underGoal = totalMinutes <= goalMinutes
//
//        when {
//            lastDate == today -> {
//                // Already checked today — just update if under goal
//                if (!underGoal) {
//                    // Failed today — reset streak
//                    currentStreak = 0
//                }
//            }
//            lastDate == getYesterdayString() -> {
//                // Checked yesterday — continue or break streak
//                if (underGoal) {
//                    currentStreak += 1
//                } else {
//                    currentStreak = 0
//                }
//            }
//            else -> {
//                // Missed a day — reset streak
//                currentStreak = if (underGoal) 1 else 0
//            }
//        }
//
//        // Update best streak
//        if (currentStreak > bestStreak) {
//            bestStreak = currentStreak
//        }
//
//        prefs.edit()
//            .putInt(KEY_STREAK, currentStreak)
//            .putInt(KEY_BEST_STREAK, bestStreak)
//            .putString(KEY_LAST_DATE, today)
//            .apply()
//    }
//
//    fun getStreak(context: Context): Int {
//        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//            .getInt(KEY_STREAK, 0)
//    }
//
//    fun getBestStreak(context: Context): Int {
//        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//            .getInt(KEY_BEST_STREAK, 0)
//    }
//
//    fun getStreakEmoji(streak: Int): String {
//        return when {
//            streak >= 30 -> "🔥🔥🔥"
//            streak >= 14 -> "🔥🔥"
//            streak >= 7  -> "🔥"
//            streak >= 3  -> "⚡"
//            streak >= 1  -> "✅"
//            else         -> "❌"
//        }
//    }
//
//    private fun getTodayString(): String {
//        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
//        return sdf.format(java.util.Date())
//    }
//
//    private fun getYesterdayString(): String {
//        val cal = java.util.Calendar.getInstance()
//        cal.add(java.util.Calendar.DATE, -1)
//        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
//        return sdf.format(cal.time)
//    }
//}


package com.example.screentimetracker

import android.content.Context

object StreakHelper {

    private const val PREFS = "streak_prefs"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LAST_DATE = "last_checked_date"
    private const val KEY_BEST_STREAK = "best_streak"
    private const val POINTS_GOAL = 1

    fun updateStreak(context: Context, todayPoints: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = getTodayString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        var currentStreak = prefs.getInt(KEY_STREAK, 0)
        var bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)

        val metGoal = todayPoints > 0  // positive points = streak continues

        when {
            lastDate == today -> {
                // Already checked today — update based on points
                if (!metGoal) currentStreak = 0
            }
            lastDate == getYesterdayString() -> {
                // Checked yesterday — continue or break streak
                currentStreak = if (metGoal) currentStreak + 1 else 0
            }
            else -> {
                // Missed a day or first time — reset
                currentStreak = if (metGoal) 1 else 0
            }
        }

        if (currentStreak > bestStreak) bestStreak = currentStreak

        prefs.edit()
            .putInt(KEY_STREAK, currentStreak)
            .putInt(KEY_BEST_STREAK, bestStreak)
            .putString(KEY_LAST_DATE, today)
            .apply()
    }

    fun getStreak(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_STREAK, 0)
    }

    fun getBestStreak(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_BEST_STREAK, 0)
    }

    fun getStreakEmoji(streak: Int): String {
        return when {
            streak >= 30 -> "🔥🔥🔥"
            streak >= 14 -> "🔥🔥"
            streak >= 7  -> "🔥"
            streak >= 3  -> "⚡"
            streak >= 1  -> "✅"
            else         -> "❌"
        }
    }

    fun getPointsGoal() =  POINTS_GOAL

    private fun getTodayString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun getYesterdayString(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(cal.time)
    }
}