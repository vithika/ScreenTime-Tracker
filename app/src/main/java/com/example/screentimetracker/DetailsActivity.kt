package com.example.screentimetracker

import android.os.Bundle
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

        recyclerProductive.layoutManager   = LinearLayoutManager(this)
        recyclerEntertaining.layoutManager = LinearLayoutManager(this)

        loadData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val appList          = UsageHelper.getAppUsageList(this@DetailsActivity)
            val productiveList   = ArrayList<AppUsageModel>()
            val entertainingList = ArrayList<AppUsageModel>()

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
                recyclerProductive.adapter   = AppUsageAdapter(productiveList)
                recyclerEntertaining.adapter = AppUsageAdapter(entertainingList)
            }
        }
    }
}