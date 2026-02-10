package com.example.sysinfo.usage

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

object UsageStatsHelper {
    
    fun isUsageStatsPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    fun requestUsageStatsPermission(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("需要使用情况访问权限")
            .setMessage("请前往设置开启权限")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                activity.startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}