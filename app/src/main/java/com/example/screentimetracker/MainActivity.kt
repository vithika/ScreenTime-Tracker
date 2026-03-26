package com.example.screentimetracker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screentimetracker.model.AppUsageModel
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var barChart: BarChart


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTime = findViewById(R.id.tvTime)
        barChart = findViewById(R.id.barChart)
        recycler = findViewById(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)

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

            val list = ArrayList<AppUsageModel>()

            for (info in appList) {
                val icon = UsageHelper.getAppIcon(this@MainActivity, info.packageName)
                list.add(AppUsageModel( info.appName, info.usageTimeMs,icon, info.isInstalled))
            }

            withContext(Dispatchers.Main) {
                val minutes = totalMs / (1000 * 60)
                tvTime.text = "$minutes minutes screen time"
                recycler.adapter = AppUsageAdapter(list)
                setupChart(weeklyData, dayLabels)
            }
        }
    }


    private fun setupChart(weeklyData: FloatArray, dayLabels: Array<String>) {
        val entries = ArrayList<BarEntry>()
        for (i in weeklyData.indices) {
            entries.add(BarEntry(i.toFloat(), weeklyData[i]))
        }

        val dataSet = BarDataSet(entries, "Screen Time (mins)").apply {
            color = getColor(R.color.black)
            valueTextSize = 10f
            setDrawValues(true)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        barChart.apply {
            data = barData

            // X axis — day labels
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(dayLabels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 11f
            }

            // Y axis
            axisLeft.apply {
                granularity = 1f
                axisMinimum = 0f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false

            // Chart settings
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            animateY(800)
            invalidate()
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
}


