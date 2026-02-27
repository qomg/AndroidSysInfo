package com.example.sysinfo.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sysinfo.R
import com.example.sysinfo.usage.AppUsageAnalyzer
import com.example.sysinfo.usage.UsageStatsHelper
import com.example.sysinfo.usage.UsageStatsPermissionHelper
import com.example.sysinfo.usage.UsageStatsPermissionManager

class UsageActivity : AppCompatActivity(R.layout.activity_usage) {

    private val helper = UsageStatsPermissionHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println(UsageStatsPermissionManager(this).getPermissionExplanation())

        helper.checkAndRequestPermission({
            analyzeUsage()
        }, {
            Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        helper.handleActivityResult(requestCode, 0, null)
    }

    override fun onResume() {
        super.onResume()
        return
        if (AppUsageAnalyzer.isSupported(this)) {
            window?.decorView?.postDelayed({
                if (UsageStatsHelper.isUsageStatsPermissionGranted(this)) {
                    analyzeUsage()
                } else {
                    UsageStatsHelper.requestUsageStatsPermission(this)
                }
            }, 500)
        }
    }

    private fun analyzeUsage() {
        val analyzer = AppUsageAnalyzer(this)
        val topUsedApps = analyzer.getTopUsedApps()
        findViewById<TextView>(R.id.topUsedApps).text = topUsedApps.joinToString {
            it.toString()
        }
        val topBatteryDrainingApps = analyzer.getTopBatteryDrainingApps()
        findViewById<TextView>(R.id.topBatteryDrainingApps).text = topBatteryDrainingApps.joinToString {
            it.toString()
        }
        val userApps = analyzer.getUserAppsOnly()
        findViewById<TextView>(R.id.userApps).text = userApps.joinToString {
            it.toString()
        }
        val systemApps = analyzer.getSystemAppsOnly()
        findViewById<TextView>(R.id.systemApps).text = systemApps.joinToString {
            it.toString()
        }
        val category = analyzer.getUsageByCategory()
        findViewById<TextView>(R.id.category).text = category.entries.joinToString { (key, value) ->
            "$key=$value"
        }
        val summary = analyzer.getUsageSummary()
        findViewById<TextView>(R.id.summary).text = summary.entries.joinToString { (key, value) ->
            "$key=$value"
        }
    }
}