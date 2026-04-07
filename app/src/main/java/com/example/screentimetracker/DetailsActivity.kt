package com.example.screentimetracker

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screentimetracker.model.AppUsageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsActivity : AppCompatActivity() {

    private lateinit var recyclerProductive: RecyclerView
    private lateinit var recyclerEntertaining: RecyclerView
    private lateinit var recyclerHeatmap: RecyclerView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var mainContent: ScrollView
    private lateinit var tvLoadingMessage: TextView
    private lateinit var tvLoadingSubMessage: TextView


    private val productiveCategories = setOf(
        AppCategory.EDUCATION, AppCategory.PRODUCTIVITY,
        AppCategory.FINANCE, AppCategory.HEALTH, AppCategory.TOOLS
    )
    private val entertainingCategories = setOf(
        AppCategory.ENTERTAINMENT, AppCategory.SOCIAL, AppCategory.GAMES,
        AppCategory.NEWS, AppCategory.SHOPPING, AppCategory.TRAVEL, AppCategory.FOOD
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        supportActionBar?.title = "App Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerProductive   = findViewById(R.id.recyclerProductive)
        recyclerEntertaining = findViewById(R.id.recyclerEntertaining)
        recyclerHeatmap      = findViewById(R.id.recyclerHeatmap)


        recyclerProductive.layoutManager   = LinearLayoutManager(this)
        recyclerEntertaining.layoutManager = LinearLayoutManager(this)
        recyclerHeatmap.layoutManager = LinearLayoutManager(this)
        loadingOverlay      = findViewById(R.id.loadingOverlay)
        mainContent         = findViewById(R.id.mainContent)
        tvLoadingMessage    = findViewById(R.id.tvLoadingMessage)
        tvLoadingSubMessage = findViewById(R.id.tvLoadingSubMessage)


        loadData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadData() {

        // Show loading, hide content
        loadingOverlay.alpha= 1f
        loadingOverlay.visibility = View.VISIBLE
        mainContent.visibility    = View.GONE

        // Rotate loading messages every second
        val handler  = android.os.Handler(mainLooper)
        val messages = LoadingMessages.messages
        var msgIndex = 0

        val messageRunnable = object : Runnable {
            override fun run() {
                tvLoadingSubMessage.text = messages[msgIndex % messages.size]
                msgIndex++
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(messageRunnable)
        lifecycleScope.launch(Dispatchers.IO) {
            val appList          = UsageHelper.getAppUsageList(this@DetailsActivity)
            val totalMs = UsageHelper.getTodayUsage(this@DetailsActivity)
            val productiveList   = ArrayList<AppUsageModel>()
            val entertainingList = ArrayList<AppUsageModel>()
            val hourlyData       = HourlyUsageHelper.getHourlyUsage(this@DetailsActivity)

            for (info in appList) {
                val icon     = UsageHelper.getAppIcon(this@DetailsActivity, info.packageName)
                val category = CategoryHelper.getCategory(this@DetailsActivity, info.packageName)
                val model    = AppUsageModel(info.appName, info.usageTimeMs, icon, info.isInstalled, category)

                when (category) {
                    in productiveCategories   -> productiveList.add(model)
                    in entertainingCategories -> entertainingList.add(model)
                    else                      -> entertainingList.add(model)
                }
            }

            productiveList.sortByDescending { it.time }
            entertainingList.sortByDescending { it.time }

            withContext(Dispatchers.Main) {


                // Stop rotating messages
                handler.removeCallbacksAndMessages(null)
                recyclerProductive.adapter   = AppUsageAdapter(productiveList,totalMs)
                recyclerEntertaining.adapter = AppUsageAdapter(entertainingList,totalMs)
                recyclerHeatmap.adapter      = HeatmapAdapter(hourlyData)


                mainContent.alpha     = 0f
                mainContent.visibility = View.VISIBLE
                mainContent.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .start()
                loadingOverlay.animate()
                    .alpha(0f)
                    .setDuration(600)
                    .withEndAction { loadingOverlay.visibility = View.GONE }
                    .start()
            }
        }
    }
}