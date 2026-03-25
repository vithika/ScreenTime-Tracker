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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTime = findViewById(R.id.tvTime)
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

            val list = ArrayList<AppUsageModel>()

            for (info in appList) {
                val icon = UsageHelper.getAppIcon(this@MainActivity, info.packageName)
                list.add(AppUsageModel( info.appName, info.usageTimeMs,icon, info.isInstalled))
            }

            withContext(Dispatchers.Main) {
                val minutes = totalMs / (1000 * 60)
                tvTime.text = "$minutes minutes screen time"
                recycler.adapter = AppUsageAdapter(list)
            }
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


