package com.example.screentimetracker


import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screentimetracker.model.AppUsageModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView

    private lateinit var barChart: BarChart

    private val PREFS = "screen_time_prefs"
    private val KEY_GOAL = "daily_goal_minutes"

    private lateinit var tvPoints: TextView
    private lateinit var tvBadge: TextView
    private lateinit var tvProductivePoints: TextView
    private lateinit var tvEntertainingPoints: TextView

    private lateinit var tvStreak: TextView
    private lateinit var tvStreakEmoji: TextView
    private lateinit var tvBestStreak: TextView
    private lateinit var pieChart: PieChart
    private lateinit var ivTopProductiveIcon: ImageView
    private lateinit var tvTopProductiveName: TextView
    private lateinit var tvTopProductiveTime: TextView
    private lateinit var ivTopEntertainingIcon: ImageView
    private lateinit var tvTopEntertainingName: TextView
    private lateinit var tvTopEntertainingTime: TextView
    private lateinit var tvFirstPickup: TextView

    private lateinit var tvScreenFree: TextView
    private lateinit var tvThisWeek: TextView
    private lateinit var tvLastWeek: TextView
    private lateinit var tvWeeklyArrow: TextView
    private lateinit var tvWeeklyDiff: TextView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var mainContent: ScrollView
    private lateinit var tvLoadingMessage: TextView
    private lateinit var tvLoadingSubMessage: TextView




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

        tvPoints             = findViewById(R.id.tvPoints)
        tvBadge              = findViewById(R.id.tvBadge)
        tvProductivePoints   = findViewById(R.id.tvProductivePoints)
        tvEntertainingPoints = findViewById(R.id.tvEntertainingPoints)
        tvStreak      = findViewById(R.id.tvStreak)
        tvStreakEmoji = findViewById(R.id.tvStreakEmoji)
        tvBestStreak  = findViewById(R.id.tvBestStreak)
        pieChart             = findViewById(R.id.pieChart)
        ivTopProductiveIcon   = findViewById(R.id.ivTopProductiveIcon)
        tvTopProductiveName   = findViewById(R.id.tvTopProductiveName)
        tvTopProductiveTime   = findViewById(R.id.tvTopProductiveTime)
        ivTopEntertainingIcon = findViewById(R.id.ivTopEntertainingIcon)
        tvTopEntertainingName = findViewById(R.id.tvTopEntertainingName)
        tvTopEntertainingTime = findViewById(R.id.tvTopEntertainingTime)
        tvFirstPickup = findViewById(R.id.tvFirstPickup)
        tvScreenFree = findViewById(R.id.tvScreenFree)
        tvThisWeek    = findViewById(R.id.tvThisWeek)
        tvLastWeek    = findViewById(R.id.tvLastWeek)
        tvWeeklyArrow = findViewById(R.id.tvWeeklyArrow)
        tvWeeklyDiff  = findViewById(R.id.tvWeeklyDiff)
        loadingOverlay      = findViewById(R.id.loadingOverlay)
        mainContent         = findViewById(R.id.mainContent)
        tvLoadingMessage    = findViewById(R.id.tvLoadingMessage)
        tvLoadingSubMessage = findViewById(R.id.tvLoadingSubMessage)


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

        findViewById<android.widget.Button>(R.id.btnDetails).setOnClickListener {
            startActivity(Intent(this, DetailsActivity::class.java))
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




            val totalMs = UsageHelper.getTodayUsage(this@MainActivity)
            val appList = UsageHelper.getAppUsageList(this@MainActivity)
            val weeklyData = WeeklyStatsHelper.getLast7DaysUsage(this@MainActivity)
            val dayLabels = WeeklyStatsHelper.getDayLabels()
            val lastWeekData   = WeeklyStatsHelper.getLastWeekUsage(this@MainActivity)
            val thisWeekAvg    = WeeklyStatsHelper.getWeeklyAverage(weeklyData)
            val lastWeekAvg    = WeeklyStatsHelper.getWeeklyAverage(lastWeekData)
            val firstPickup  = UsageHelper.getFirstPickupTime(this@MainActivity)
            val screenFree   = UsageHelper.getScreenFreeTime(this@MainActivity, totalMs) // ← add

            var totalProductiveMs = 0L;
            var totalEntertainingMs = 0L;


            val list = ArrayList<AppUsageModel>()

            val productiveList    = ArrayList<AppUsageModel>()
            val entertainingList  = ArrayList<AppUsageModel>()

            var topProductiveModel:   AppUsageModel? = null
            var topEntertainingModel: AppUsageModel? = null
            for (info in appList) {

                val icon = UsageHelper.getAppIcon(this@MainActivity, info.packageName)
                val category = CategoryHelper.getCategory(this@MainActivity, info.packageName)
                val model    = AppUsageModel(info.appName, info.usageTimeMs, icon, info.isInstalled,category)


                list.add(model)

                when (category) {

                    in productiveCategories   ->{  productiveList.add(model)
                        if (topProductiveModel == null ||
                            info.usageTimeMs > (topProductiveModel?.time ?: 0)) {
                            topProductiveModel = model
                        }
                    }
                    in entertainingCategories -> {entertainingList.add(model)
                        if (topEntertainingModel == null ||
                            info.usageTimeMs > (topEntertainingModel?.time ?: 0)) {
                            topEntertainingModel = model
                        }}
                    else                      -> entertainingList.add(model)
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

Log.d("mins",totalMs.toString())

                tvTime.text = if (hours > 0) "${hours}h ${mins}m screen time" else "${mins}m screen time"
                tvFirstPickup.text = firstPickup
                tvScreenFree.text  = screenFree

                if (totalMinutes >= goalMinutes) {
                    NotificationHelper.sendGoalExceededNotification(this@MainActivity, goalMinutes)
                }

           //     recycler.adapter = AppUsageAdapter(list)                                                                                                                                                              swwaesv
                setupChart(weeklyData, dayLabels)



                setupPieChart(totalProductiveMs, totalEntertainingMs)





                // 1. Calculate and SAVE points first
                RewardHelper.calculateAndSavePoints(this@MainActivity, totalProductiveMs, totalEntertainingMs)

                // 2. NOW read the saved points
                val points = RewardHelper.getPoints(this@MainActivity)

                // 3. THEN update streak using the fresh points
                StreakHelper.updateStreak(this@MainActivity, points)

                // 4. Read streak values
                val currentStreak = StreakHelper.getStreak(this@MainActivity)
                val bestStreak    = StreakHelper.getBestStreak(this@MainActivity)
                val streakEmoji   = StreakHelper.getStreakEmoji(currentStreak)

                // 5. Update UI
                tvStreak.text      = "$currentStreak days"
                tvStreakEmoji.text = streakEmoji
                tvBestStreak.text  = "$bestStreak days"
                tvPoints.text = "$points points today"


                // Weekly comparison
                tvThisWeek.text = WeeklyStatsHelper.formatMinutes(thisWeekAvg)
                tvLastWeek.text = WeeklyStatsHelper.formatMinutes(lastWeekAvg)

                val diff = thisWeekAvg - lastWeekAvg
                when {
                    diff > 0 -> {
                        // More screen time this week = bad
                        tvWeeklyArrow.text = "📈"
                        tvWeeklyDiff.text  = "+${WeeklyStatsHelper.formatMinutes(diff)}"
                        tvWeeklyDiff.setTextColor(getColor(R.color.bar_red))
                    }
                    diff < 0 -> {
                        // Less screen time this week = good
                        tvWeeklyArrow.text = "📉"
                        tvWeeklyDiff.text  = "-${WeeklyStatsHelper.formatMinutes(-diff)}"
                        tvWeeklyDiff.setTextColor(getColor(R.color.bar_green))
                    }
                    else -> {
                        tvWeeklyArrow.text = "➡️"
                        tvWeeklyDiff.text  = "Same"
                        tvWeeklyDiff.setTextColor(getColor(android.R.color.darker_gray))
                    }
                }

                val badge  = RewardHelper.getBadge(points)

                val productiveMin   = totalProductiveMs   / (1000 * 60)
                val entertainingMin = totalEntertainingMs / (1000 * 60)


                tvBadge.text              = badge
                tvEntertainingPoints.text = "-${entertainingMin} from entertaining"
                tvProductivePoints.text = if (points > 0) {
                    "+${productiveMin * 1} from productive"
                } else {
                    "Need ${StreakHelper.getPointsGoal()} pts to keep streak"
                }


                updateWidget()

    // ── Hide loader, show content with fade ──────────────
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

                // Trophy card — top productive app
                topProductiveModel?.let { top ->
                    tvTopProductiveName.text = top.appName
                    val mins  = top.time / (1000 * 60)
                    val hours = mins / 60
                    val rem   = mins % 60
                    tvTopProductiveTime.text = if (hours > 0) "${hours}h ${rem}m" else "${mins}m"
                    top.icon?.let { ivTopProductiveIcon.setImageDrawable(it) }
                } ?: run {
                    tvTopProductiveName.text = "No data"
                    tvTopProductiveTime.text = ""
                }

// Trophy card — top entertaining app
                topEntertainingModel?.let { top ->
                    tvTopEntertainingName.text = top.appName
                    val mins  = top.time / (1000 * 60)
                    val hours = mins / 60
                    val rem   = mins % 60
                    tvTopEntertainingTime.text = if (hours > 0) "${hours}h ${rem}m" else "${mins}m"
                    top.icon?.let { ivTopEntertainingIcon.setImageDrawable(it) }
                } ?: run {
                    tvTopEntertainingName.text = "No data"
                    tvTopEntertainingTime.text = ""
                }

            }
        }
    }

//    private fun loadUsageData() {
//        // Show loading, hide content
//        loadingOverlay.alpha= 1f
//        loadingOverlay.visibility = View.VISIBLE
//        mainContent.visibility    = View.GONE
//
//        // Rotate loading messages every second
//        val handler  = android.os.Handler(mainLooper)
//        val messages = LoadingMessages.messages
//        var msgIndex = 0
//
//        val messageRunnable = object : Runnable {
//            override fun run() {
//                tvLoadingSubMessage.text = messages[msgIndex % messages.size]
//                msgIndex++
//                handler.postDelayed(this, 1000)
//            }
//        }
//        handler.post(messageRunnable)
//
//        lifecycleScope.launch(Dispatchers.IO) {
//
//            // ── Load ALL data in background ──────────────────────────
//            val appList     = UsageHelper.getAppUsageList(this@MainActivity)
//            val totalMs     = UsageHelper.getTodayUsage(this@MainActivity)
//            val weeklyData  = WeeklyStatsHelper.getLast7DaysUsage(this@MainActivity)
//            val dayLabels   = WeeklyStatsHelper.getDayLabels()
//            val lastWeekData = WeeklyStatsHelper.getLastWeekUsage(this@MainActivity)
//            val firstPickup = UsageHelper.getFirstPickupTime(this@MainActivity)
//            val screenFree  = UsageHelper.getScreenFreeTime(this@MainActivity, totalMs)
//
//            var totalProductiveMs   = 0L
//            var totalEntertainingMs = 0L
//            var totalOtherMs        = 0L
//
//            var topProductiveModel:   AppUsageModel? = null
//            var topEntertainingModel: AppUsageModel? = null
//
//            val productiveList   = ArrayList<AppUsageModel>()
//            val entertainingList = ArrayList<AppUsageModel>()
//            val list             = ArrayList<AppUsageModel>()
//
//            for (info in appList) {
//                val icon     = UsageHelper.getAppIcon(this@MainActivity, info.packageName)
//                val category = CategoryHelper.getCategory(this@MainActivity, info.packageName)
//                val model    = AppUsageModel(info.appName, info.usageTimeMs, icon, info.isInstalled, category)
//
//                list.add(model)
//
//                when (category) {
//                    in productiveCategories -> {
//                        productiveList.add(model)
//                        totalProductiveMs += info.usageTimeMs
//                        if (topProductiveModel == null ||
//                            info.usageTimeMs > (topProductiveModel?.time ?: 0)) {
//                            topProductiveModel = model
//                        }
//                    }
//                    in entertainingCategories -> {
//                        entertainingList.add(model)
//                        totalEntertainingMs += info.usageTimeMs
//                        if (topEntertainingModel == null ||
//                            info.usageTimeMs > (topEntertainingModel?.time ?: 0)) {
//                            topEntertainingModel = model
//                        }
//                    }
//                    else -> {
//                        entertainingList.add(model)
//                        totalEntertainingMs += info.usageTimeMs
//                        totalOtherMs += info.usageTimeMs
//                    }
//                }
//            }
//
//            list.sortByDescending             { it.time }
//            productiveList.sortByDescending   { it.time }
//            entertainingList.sortByDescending { it.time }
//
//            val thisWeekAvg = WeeklyStatsHelper.getWeeklyAverage(weeklyData)
//            val lastWeekAvg = WeeklyStatsHelper.getWeeklyAverage(lastWeekData)
//
//            // ── All data ready — update UI ───────────────────────────
//            withContext(Dispatchers.Main) {
//
//                // Stop rotating messages
//                handler.removeCallbacksAndMessages(null)
//
//                // Screen time text
//                val totalMinutes = totalMs / (1000 * 60)
//                val hours = totalMinutes / 60
//                val mins  = totalMinutes % 60
//                tvTime.text = if (hours > 0) "${hours}h ${mins}m screen time" else "${mins}m screen time"
//
//                // First pickup + screen free
//                tvFirstPickup.text = firstPickup
//                tvScreenFree.text  = screenFree
//
//                // Charts
//                setupChart(weeklyData, dayLabels)
//                setupPieChart(totalProductiveMs, totalEntertainingMs)
//
//                // Weekly comparison
//                tvThisWeek.text = WeeklyStatsHelper.formatMinutes(thisWeekAvg)
//                tvLastWeek.text = WeeklyStatsHelper.formatMinutes(lastWeekAvg)
//                val diff = thisWeekAvg - lastWeekAvg
//                when {
//                    diff > 0 -> {
//                        tvWeeklyArrow.text = "📈"
//                        tvWeeklyDiff.text  = "+${WeeklyStatsHelper.formatMinutes(diff)}"
//                        tvWeeklyDiff.setTextColor(getColor(R.color.bar_red))
//                    }
//                    diff < 0 -> {
//                        tvWeeklyArrow.text = "📉"
//                        tvWeeklyDiff.text  = "-${WeeklyStatsHelper.formatMinutes(-diff)}"
//                        tvWeeklyDiff.setTextColor(getColor(R.color.bar_green))
//                    }
//                    else -> {
//                        tvWeeklyArrow.text = "➡️"
//                        tvWeeklyDiff.text  = "Same"
//                        tvWeeklyDiff.setTextColor(getColor(android.R.color.darker_gray))
//                    }
//                }
//
//                // Points
//                RewardHelper.calculateAndSavePoints(this@MainActivity, totalProductiveMs, totalEntertainingMs)
//                val points = RewardHelper.getPoints(this@MainActivity)
//                val badge  = RewardHelper.getBadge(points)
//                val productiveMin   = totalProductiveMs   / (1000 * 60)
//                val entertainingMin = totalEntertainingMs / (1000 * 60)
//                tvPoints.text             = "⭐ $points points today"
//                tvBadge.text              = badge
//                tvProductivePoints.text   = if (points > 0) "+${productiveMin * 2} from productive" else "Need positive points to keep streak"
//                tvEntertainingPoints.text = "-${entertainingMin} from entertaining"
//
//                // Streak
//                StreakHelper.updateStreak(this@MainActivity, points)
//                val currentStreak = StreakHelper.getStreak(this@MainActivity)
//                val bestStreak    = StreakHelper.getBestStreak(this@MainActivity)
//                tvStreak.text      = "$currentStreak days"
//                tvStreakEmoji.text = StreakHelper.getStreakEmoji(currentStreak)
//                tvBestStreak.text  = "$bestStreak days"
//
//                // Trophy card
//                topProductiveModel?.let { top ->
//                    tvTopProductiveName.text = top.appName
//                    val m  = top.time / (1000 * 60)
//                    val h  = m / 60; val r = m % 60
//                    tvTopProductiveTime.text = if (h > 0) "${h}h ${r}m" else "${m}m"
//                    top.icon?.let { ivTopProductiveIcon.setImageDrawable(it) }
//                } ?: run { tvTopProductiveName.text = "No data" }
//
//                topEntertainingModel?.let { top ->
//                    tvTopEntertainingName.text = top.appName
//                    val m  = top.time / (1000 * 60)
//                    val h  = m / 60; val r = m % 60
//                    tvTopEntertainingTime.text = if (h > 0) "${h}h ${r}m" else "${m}m"
//                    top.icon?.let { ivTopEntertainingIcon.setImageDrawable(it) }
//                } ?: run { tvTopEntertainingName.text = "No data" }
//
//                // Goal check
//                val goalMinutes = getGoalMinutes()
//                if (totalMinutes >= goalMinutes) {
//                    NotificationHelper.sendGoalExceededNotification(this@MainActivity, goalMinutes)
//                }
//
//                // Widget
//                updateWidget()
//
//                // ── Hide loader, show content with fade ──────────────
//                mainContent.alpha     = 0f
//                mainContent.visibility = View.VISIBLE
//                mainContent.animate()
//                    .alpha(1f)
//                    .setDuration(600)
//                    .start()
//                loadingOverlay.animate()
//                    .alpha(0f)
//                    .setDuration(600)
//                    .withEndAction { loadingOverlay.visibility = View.GONE }
//                    .start()
//            }
//        }
//    }
    private fun setupPieChart(productiveMs: Long, entertainingMs: Long) {
        val productiveMin   = (productiveMs   / (1000 * 60)).toFloat()
        val entertainingMin = (entertainingMs / (1000 * 60)).toFloat()

        val entries = ArrayList<PieEntry>()
        val colors  = ArrayList<Int>()

        if (productiveMin > 0) {
            entries.add(PieEntry(productiveMin, "Productive"))
            colors.add(android.graphics.Color.parseColor("#4CAF50")) // green
        }
        if (entertainingMin > 0) {
            entries.add(PieEntry(entertainingMin, "Entertaining"))
            colors.add(android.graphics.Color.parseColor("#F44336")) // red
        }


        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)              // ← pass the matched colors list
            valueTextSize  = 13f
            valueTextColor = android.graphics.Color.WHITE
            sliceSpace     = 3f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled     = true
            holeRadius            = 40f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setCenterText("Today")
            setCenterTextSize(16f)
            setEntryLabelColor(android.graphics.Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.isEnabled      = true
            animateY(800)
            invalidate()
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

    private fun updateWidget() {
        val manager = android.appwidget.AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            android.content.ComponentName(this, ScreenTimeWidget::class.java)
        )
        for (id in ids) {
            ScreenTimeWidget.updateWidget(this, manager, id)
        }
    }
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


