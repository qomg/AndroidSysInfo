package com.example.sysinfo.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.sysinfo.data.model.BatteryState
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BatteryUsageStatsHelper(
    private val context: Context,
) {
    private val usageStatsManager: UsageStatsManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        } else {
            null
        }
    }

    data class BatteryUsageInfo(
        val packageName: String,
        val totalTimeInForeground: Long,
        val lastTimeUsed: Long,
        val appLaunchCount: Int,
        val estimatedBatteryDrain: Float,
        val usagePercentage: Float,
    )

    fun getTodayBatteryUsageStats(): List<BatteryUsageInfo> = getBatteryUsageStatsForPeriod(TimeUnit.DAYS.toMillis(1))

    fun getLast7DaysBatteryUsageStats(): List<BatteryUsageInfo> = getBatteryUsageStatsForPeriod(TimeUnit.DAYS.toMillis(7))

    fun getLast24HoursBatteryUsageStats(): List<BatteryUsageInfo> = getBatteryUsageStatsForPeriod(TimeUnit.HOURS.toMillis(24))

    private fun getBatteryUsageStatsForPeriod(periodMillis: Long): List<BatteryUsageInfo> {
        if (usageStatsManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return emptyList()
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - periodMillis

        val usageStatsList =
            usageStatsManager!!.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime,
            )

        if (usageStatsList.isEmpty()) {
            Log.w("BatteryUsageStats", "No usage stats found for the specified period")
            return emptyList()
        }

        val totalUsageTime = usageStatsList.sumOf { it.totalTimeInForeground }

        return usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                val usagePercentage =
                    if (totalUsageTime > 0) {
                        (stats.totalTimeInForeground.toFloat() / totalUsageTime.toFloat()) * 100f
                    } else {
                        0f
                    }

                BatteryUsageInfo(
                    packageName = stats.packageName,
                    totalTimeInForeground = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed,
                    appLaunchCount =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            stats.appLaunchCount
                        } else {
                            0
                        },
                    estimatedBatteryDrain = estimateBatteryDrain(stats),
                    usagePercentage = usagePercentage,
                )
            }.sortedByDescending { it.totalTimeInForeground }
    }

    private fun estimateBatteryDrain(stats: UsageStats): Float {
        val hoursInForeground = stats.totalTimeInForeground / (1000f * 60f * 60f)
        val baseDrainRate = 0.05f

        val packageMultiplier =
            when {
                stats.packageName.contains("com.google.android.youtube") -> 1.5f
                stats.packageName.contains("com.netflix") -> 1.4f
                stats.packageName.contains("com.tencent.mm") -> 1.2f
                stats.packageName.contains("com.facebook") -> 1.3f
                stats.packageName.contains("com.instagram") -> 1.3f
                stats.packageName.contains("com.twitter") -> 1.2f
                stats.packageName.contains("com.tiktok") -> 1.4f
                stats.packageName.contains("com.spotify") -> 1.1f
                stats.packageName.contains("com.google.android.gm") -> 0.8f
                stats.packageName.contains("com.whatsapp") -> 1.1f
                else -> 1.0f
            }

        return hoursInForeground * baseDrainRate * packageMultiplier
    }

    fun getTopBatteryDrainingApps(limit: Int = 10): List<BatteryUsageInfo> =
        getTodayBatteryUsageStats()
            .sortedByDescending { it.estimatedBatteryDrain }
            .take(limit)

    fun getBatteryUsageByHour(): Map<Int, List<BatteryUsageInfo>> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val hourlyUsage = mutableMapOf<Int, MutableList<BatteryUsageInfo>>()

        for (hour in 0..23) {
            hourlyUsage[hour] = mutableListOf()
        }

        val todayStats = getTodayBatteryUsageStats()

        todayStats.forEach { info ->
            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis = info.lastTimeUsed
                }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyUsage[hour]?.add(info)
        }

        return hourlyUsage
    }

    fun correlateBatteryStateWithUsage(batteryState: BatteryState): List<BatteryUsageInfo> {
        val usageStats = getTodayBatteryUsageStats()

        return usageStats.map { info ->
            val correlationFactor = calculateCorrelationFactor(batteryState, info)
            info.copy(estimatedBatteryDrain = info.estimatedBatteryDrain * correlationFactor)
        }
    }

    private fun calculateCorrelationFactor(
        batteryState: BatteryState,
        usageInfo: BatteryUsageInfo,
    ): Float {
        var factor = 1.0f

        if (batteryState.temperature > 35f) {
            factor *= 1.2f
        }

        if (batteryState.level < 20) {
            factor *= 1.1f
        }

        if (!batteryState.isCharging && usageInfo.totalTimeInForeground > TimeUnit.HOURS.toMillis(2)) {
            factor *= 1.15f
        }

        if (batteryState.isPowerSave) {
            factor *= 0.8f
        }

        return factor
    }

    fun getBatteryUsageSummary(): Map<String, Any> {
        val todayStats = getTodayBatteryUsageStats()

        val totalApps = todayStats.size
        val totalUsageTime = todayStats.sumOf { it.totalTimeInForeground }
        val totalEstimatedDrain = todayStats.sumOf { it.estimatedBatteryDrain.toDouble() }
        val topDrainer = todayStats.maxByOrNull { it.estimatedBatteryDrain }

        return mapOf(
            "totalApps" to totalApps,
            "totalUsageTimeMinutes" to (totalUsageTime / (1000 * 60)),
            "totalEstimatedDrainPercent" to totalEstimatedDrain,
            "topBatteryDrainingApp" to (topDrainer?.packageName ?: "None"),
            "topDrainAmount" to (topDrainer?.estimatedBatteryDrain ?: 0f),
            "averageUsagePerApp" to (if (totalApps > 0) totalUsageTime / totalApps else 0),
        )
    }

    companion object {
        fun isUsageStatsSupported(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                context.getSystemService(Context.USAGE_STATS_SERVICE) != null
    }
}
