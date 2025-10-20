package com.example.sysinfo.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sysinfo.data.db.AppDatabase
import com.example.sysinfo.data.repo.SysInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CellBatteryWorker"

class CellAndBatteryMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repo by lazy { SysInfoRepository(context.applicationContext) }
    private val db by lazy { AppDatabase.get(context.applicationContext) }
    private val cellDao by lazy { db.cellTowerDao() }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始执行后台监测任务...")

                // 1. 采集电池信息（可选存储或上报）
                val battery = repo.battery()
                Log.d(TAG, "电池: ${battery.level}% , 温度: ${battery.temperature}°C")

                // 2. 采集网络信息（可选）
                val mobile = repo.mobile()
                Log.d(TAG, "移动网络: ${mobile.networkType}, 运营商: ${mobile.operator}")

                val wifi = repo.wifi()
                if (wifi != null) {
                    Log.d(TAG, "Wi-Fi: ${wifi.ssid}, 信号: ${wifi.rssi}dBm")
                }

                // 3. 采集小区信息（重点）
                if (hasLocationPermission(applicationContext)) {
                    val cells = repo.cellTowers()
                    Log.d(TAG, "小区数量: ${cells.size}")
                    if (cells.isNotEmpty()) {
                        cellDao.insertAll(*cells.toTypedArray())
                        Log.d(TAG, "已保存 ${cells.size} 个小区到数据库")
                    }
                } else {
                    Log.d(TAG, "缺少位置权限，跳过小区采集")
                }

                // TODO: 你可以在这里扩展采集内存、CPU 等，并存储到数据库

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "监测任务失败", e)
                Result.retry() // 失败时自动重试（有退避策略）
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}

fun oneTimeRequest(context: Context) {
    val oneTimeRequest = OneTimeWorkRequestBuilder<CellAndBatteryMonitorWorker>()
        .setConstraints(Constraints.Builder().build()) // 无约束
        .addTag("manual_cell_monitor")
        .build()

    WorkManager.getInstance(context).enqueue(oneTimeRequest)
    Toast.makeText(context, "已触发一次后台采集", Toast.LENGTH_SHORT).show()
}