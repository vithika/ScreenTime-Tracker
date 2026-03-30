package com.example.screentimetracker


import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screentimetracker.model.AppUsageModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var recyclerProductive: RecyclerView
    private lateinit var recyclerEntertaining: RecyclerView
    private lateinit var barChart: BarChart

    private val PREFS = "screen_time_prefs"
    private val KEY_GOAL = "daily_goal_minutes"

    private lateinit var tvPoints: TextView
    private lateinit var tvBadge: TextView
    private lateinit var tvProductivePoints: TextView
    private lateinit var tvEntertainingPoints: TextView




    private val productiveCategories = setOf(
        AppCategory.EDUCATION,
        AppCategory.PRODUCTIVITY,
        AppCategory.FINANCE,
        AppCategory.HEALTH,
        AppCategory.TOOLS
    )

    // Entertaining categories
    private val entertainingCategories = setOf(
        AppCategory.ENTERTAINMENT,
        AppCategory.SOCIAL,
        AppCategory.GAMES,
        AppCategory.NEWS,
        AppCategory.SHOPPING,
        AppCategory.TRAVEL,
        AppCategory.FOOD
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        NotificationHelper.createChannel(this)

        tvTime = findViewById(R.id.tvTime)
        barChart = findViewById(R.id.barChart)
        recycler = findViewById(R.id.recycler)
        recyclerProductive  = findViewById(R.id.recyclerProductive)
        recyclerEntertaining= findViewById(R.id.recyclerEntertaining)
        recycler.layoutManager = LinearLayoutManager(this)
        recyclerProductive.layoutManager = LinearLayoutManager(this)   // ← add this
        recyclerEntertaining.layoutManager = LinearLayoutManager(this) // ← add this
        tvPoints             = findViewById(R.id.tvPoints)
        tvBadge              = findViewById(R.id.tvBadge)
        tvProductivePoints   = findViewById(R.id.tvProductivePoints)
        tvEntertainingPoints = findViewById(R.id.tvEntertainingPoints)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }


// Goal button — add a button in your XML with id btnSetGoal
        findViewById<android.widget.Button>(R.id.btnSetGoal).setOnClickListener {
            showGoalDialog()
        }
        if (!hasPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadUsageData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission()) {
            loadUsageData()
        }
    }

    private fun loadUsageData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val appList = UsageHelper.getAppUsageList(this@MainActivity)
            val totalMs = UsageHelper.getTodayUsage(this@MainActivity)

            val weeklyData = WeeklyStatsHelper.getLast7DaysUsage(this@MainActivity)
            val dayLabels = WeeklyStatsHelper.getDayLabels()

            var totalProductiveMs = 0L;
            var totalEntertainingMs = 0L;

            val list = ArrayList<AppUsageModel>()

            val productiveList    = ArrayList<AppUsageModel>()
            val entertainingList  = ArrayList<AppUsageModel>()


            for (info in appList) {
                val icon = UsageHelper.getAppIcon(this@MainActivity, info.packageName)
                val category = CategoryHelper.getCategory(this@MainActivity, info.packageName)
                val model    = AppUsageModel(info.appName, info.usageTimeMs, icon, info.isInstalled,category)


                list.add(model)

                when (category) {

                    in productiveCategories   -> productiveList.add(model)
                    in entertainingCategories -> entertainingList.add(model)
                    else                      -> entertainingList.add(model)// OTHER goes to entertaining
                }

            }
            productiveList.sortByDescending { it.time }
            entertainingList.sortByDescending { it.time }

            totalProductiveMs   = productiveList.sumOf  { it.time }
            totalEntertainingMs = entertainingList.sumOf { it.time }

            withContext(Dispatchers.Main) {

                val totalMinutes = totalMs / (1000 * 60)
                val goalMinutes = getGoalMinutes()
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60


                tvTime.text = if (hours > 0) "${hours}h ${mins}m screen time" else "${mins}m screen time"

                if (totalMinutes >= goalMinutes) {
                    NotificationHelper.sendGoalExceededNotification(this@MainActivity, goalMinutes)
                }

           //     recycler.adapter = AppUsageAdapter(list)
                setupChart(weeklyData, dayLabels)

                recyclerProductive.adapter   = AppUsageAdapter(productiveList)
                recyclerEntertaining.adapter = AppUsageAdapter(entertainingList)


                // Calculate and display points
                RewardHelper.calculateAndSavePoints(
                    this@MainActivity,
                    totalProductiveMs,
                    totalEntertainingMs
                )
                // In MainActivity, just pass the raw Ms values directly
                RewardHelper.calculateAndSavePoints(this@MainActivity, totalProductiveMs, totalEntertainingMs)

                val points = RewardHelper.getPoints(this@MainActivity)
                val badge  = RewardHelper.getBadge(points)

                val productiveMin   = totalProductiveMs   / (1000 * 60)
                val entertainingMin = totalEntertainingMs / (1000 * 60)

                tvPoints.text             = "$points points today"
                tvBadge.text              = badge
                tvProductivePoints.text   = "+${productiveMin * 1} from productive"
                tvEntertainingPoints.text = "-${entertainingMin} from entertaining"



            }
        }
    }


    private fun setupChart(weeklyData: FloatArray, dayLabels: Array<String>) {
        val entries = ArrayList<BarEntry>()
        for (i in weeklyData.indices) {
            entries.add(BarEntry(i.toFloat(), weeklyData[i]))
        }

        // Color each bar based on usage
        val maxUsage = weeklyData.max()
        val colors = weeklyData.map { value ->
            when {
                value <= maxUsage * 0.33f -> getColor(R.color.bar_green)  // low
                value <= maxUsage * 0.66f -> getColor(R.color.bar_yellow) // medium
                else                      -> getColor(R.color.bar_red)    // high
            }
        }

        val dataSet = BarDataSet(entries, "Screen Time (mins)").apply {
            setColors(colors)   // ← per-bar colors
            valueTextSize = 10f
            setDrawValues(true)
        }

        val barData = BarData(dataSet).apply { barWidth = 0.6f }

        barChart.apply {
            data = barData
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(dayLabels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 11f
            }
            axisLeft.apply {
                granularity = 1f
                axisMinimum = 0f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            animateY(800)
            invalidate()
        }
    }

//    private fun setupChart(weeklyData: FloatArray, dayLabels: Array<String>) {
//        val entries = ArrayList<BarEntry>()
//        for (i in weeklyData.indices) {
//            entries.add(BarEntry(i.toFloat(), weeklyData[i]))
//        }
//
//        val dataSet = BarDataSet(entries, "Screen Time (mins)").apply {
//            color = getColor(R.color.black)
//            valueTextSize = 10f
//            setDrawValues(true)
//        }
//
//        val barData = BarData(dataSet).apply {
//            barWidth = 0.6f
//        }
//
//        barChart.apply {
//            data = barData
//
//            // X axis — day labels
//            xAxis.apply {
//                valueFormatter = IndexAxisValueFormatter(dayLabels)
//                position = XAxis.XAxisPosition.BOTTOM
//                granularity = 1f
//                setDrawGridLines(false)
//                textSize = 11f
//            }
//
//            // Y axis
//            axisLeft.apply {
//                granularity = 1f
//                axisMinimum = 0f
//                setDrawGridLines(true)
//            }
//            axisRight.isEnabled = false
//
//            // Chart settings
//            description.isEnabled = false
//            legend.isEnabled = false
//            setTouchEnabled(true)
//            setPinchZoom(false)
//            animateY(800)
//            invalidate()
//        }
//    }
//
        private fun hasPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getGoalMinutes(): Int {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_GOAL, 120) // default 2 hours
    }

    private fun saveGoalMinutes(minutes: Int) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_GOAL, minutes).apply()
    }

    private fun showGoalDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Enter goal in minutes"
            setText(getGoalMinutes().toString())
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Set Daily Screen Time Goal")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val goal = input.text.toString().toIntOrNull()
                if (goal != null && goal > 0) {
                    saveGoalMinutes(goal)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}


