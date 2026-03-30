package com.example.screentimetracker

import android.content.Context
import android.util.Log

object RewardHelper {

    private const val PREFS = "reward_prefs"
    private const val KEY_POINTS = "total_points"

    // Points per minute
    private const val POINTS_PER_MIN_PRODUCTIVE   =  1
    private const val POINTS_PER_MIN_ENTERTAINING = 1

    fun calculateAndSavePoints(context: Context, productiveMs: Long, entertainingMs: Long) {
        val productiveMin   = productiveMs   / (1000 * 60)
        val entertainingMin = entertainingMs / (1000 * 60)

        val earned  = productiveMin   * POINTS_PER_MIN_PRODUCTIVE
        val deducted = entertainingMin * POINTS_PER_MIN_ENTERTAINING

        Log.d("RewardDebug", "productiveMin=$productiveMin earned=$earned")
        Log.d("RewardDebug", "entertainingMin=$entertainingMin deducted=$deducted")
        Log.d("RewardDebug", "total before clamp=${earned - deducted}")

        val total = (earned - deducted) // never go below 0
        Log.d("RewardDebug", "final points=$total")
        savePoints(context, total.toInt())
    }

    fun getPoints(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_POINTS, 0)
    }

    private fun savePoints(context: Context, points: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_POINTS, points).apply()
    }

    fun getBadge(points: Int): String {
        return when {
            points >= 200 -> "🏆 Gold"
            points >= 100 -> "🥈 Silver"
            points >= 50  -> "🥉 Bronze"
            else          -> "🌱 Beginner"
        }
    }
}