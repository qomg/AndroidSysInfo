package com.example.sysinfo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.sysinfo.SysInfoApp
import com.example.sysinfo.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 后台服务，用于定时采集系统信息
 */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo get() = (application as SysInfoApp).repository
    private val db get() = AppDatabase.get(this)
    private var lastCellTs = 0L

    // 如果你不需要跨进程 Binder，直接返回 null 或者不重写 onBind
    override fun onBind(intent: Intent?): IBinder? {
        return null  // 普通本地 Service，不需要客户端绑定
    }

    override fun onCreate() {
        super.onCreate()
        val chan =
            NotificationChannel(
                CHANNEL_ID,
                "System Info Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            chan
        )
        startForeground(
            1, NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("系统监测中").setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        )
        startMonitoring()
    }

    private fun startMonitoring() = scope.launch {
        while (true) {
            val towers = repo.cellTowers()
            if (towers.isNotEmpty()) {
                val now = System.currentTimeMillis()
                if (now - lastCellTs > 10_000) { // 10s 节流
                    db.cellTowerDao().insertAll(*towers.toTypedArray())
                    lastCellTs = now
                }
            }
            delay(2_000) // 2s 采集一次
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "sysinfo"
    }
}