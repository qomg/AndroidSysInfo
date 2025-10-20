package com.example.sysinfo

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sysinfo.data.repo.SysInfoRepository
import com.example.sysinfo.worker.CellAndBatteryMonitorWorker
import java.util.concurrent.TimeUnit

class SysInfoApp : Application() {

    lateinit var repository: SysInfoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = SysInfoRepository(this)

        // 启动 WorkManager 定时任务
        setupPeriodicWork()
    }

    private fun setupPeriodicWork() {
        val workManager = WorkManager.getInstance(this)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 可选：有网络时才执行
            .setRequiresBatteryNotLow(true)                 // 可选：非低电量时执行
            // .setRequiresCharging(true)                   // 如需充电时才执行，可注释掉
            .build()

        val monitorRequest = PeriodicWorkRequestBuilder<CellAndBatteryMonitorWorker>(
            // 重复间隔：15分钟（最小间隔是 15 分钟，除非使用 setInitialDelay + 不规则间隔）
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("cell_battery_monitor")
            .build()

        // 使用 replace 原有任务（避免重复）
        workManager.enqueueUniquePeriodicWork(
            "CellAndBatteryMonitorWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            monitorRequest
        )

        Log.d("SysInfoApp", "已启动后台定时监测任务（每15分钟）")
    }
}