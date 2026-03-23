package com.example.screentimetracker


import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screentimetracker.model.AppUsageModel
//import com.github.mikephil.charting.charts.BarChart
//import com.github.mikephil.charting.data.BarData
//import com.github.mikephil.charting.data.BarDataSet
//import com.github.mikephil.charting.data.BarEntry

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var recycler: RecyclerView
   // private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTime = findViewById(R.id.tvTime)
        recycler = findViewById(R.id.recycler)
       // barChart = findViewById(R.id.barChart)

        checkPermission()

        val millis = UsageHelper.getTodayUsage(this)
        val minutes = millis / (1000 * 60)

        tvTime.text = "$minutes minutes screen time"

        val usageMap = UsageHelper.getAppUsageList(this)

        val list = ArrayList<AppUsageModel>()
        val pm = packageManager

        for ((pkg, time) in usageMap) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name = pm.getApplicationLabel(appInfo).toString()

                list.add(AppUsageModel(name, time))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AppUsageAdapter(list)

        //setupChart()
    }

    private fun checkPermission() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )

        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

//    private fun setupChart() {
//        val entries = ArrayList<BarEntry>()
//
//        entries.add(BarEntry(0f, 10f))
//        entries.add(BarEntry(1f, 20f))
//        entries.add(BarEntry(2f, 5f))
//        entries.add(BarEntry(3f, 30f))
//
//        val dataSet = BarDataSet(entries, "Weekly Usage")
//        val data = BarData(dataSet)
//
//        barChart.data = data
//        barChart.invalidate()
//    }
}
