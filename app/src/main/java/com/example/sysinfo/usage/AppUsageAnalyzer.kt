package com.example.sysinfo.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Analyzes app usage data for the last 7 days
 */
class AppUsageAnalyzer(private val context: Context) {
    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    /**
     * 分析今天的使用情况
     */
    fun analyzeTodayUsage(): List<AppUsageAnalysis> = analyzeUsageForPeriod(TimeUnit.DAYS.toMillis(1))

    /**
     * 分析最近7天的使用情况
     */
    fun analyzeLast7DaysUsage(): List<AppUsageAnalysis> = analyzeUsageForPeriod(TimeUnit.DAYS.toMillis(7))

    /**
     * 分析最近30天的使用情况
     */
    fun analyzeLast30DaysUsage(): List<AppUsageAnalysis> = analyzeUsageForPeriod(TimeUnit.DAYS.toMillis(30))

    /**
     * 分析指定时间段的使用情况
     */
    private fun analyzeUsageForPeriod(periodMillis: Long): List<AppUsageAnalysis> {
        if (usageStatsManager == null) {
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

        return usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                val appName = getAppName(stats.packageName)
                val isSystemApp = isSystemApp(stats.packageName)
                val usageFrequency = calculateUsageFrequency(stats, periodMillis)
                val batteryImpactScore = calculateBatteryImpactScore(stats, isSystemApp)

                AppUsageAnalysis(
                    packageName = stats.packageName,
                    appName = appName,
                    isSystemApp = isSystemApp,
                    totalTimeInForeground = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed,
                    appLaunchCount = 0,
                    averageSessionTime = 0,
                    usageFrequency = usageFrequency,
                    batteryImpactScore = batteryImpactScore,
                )
            }.sortedByDescending { it.totalTimeInForeground }
    }

    /**
     * 获取应用名称
     */
    private fun getAppName(packageName: String): String =
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

    /**
     * 判断应用是否是系统应用
     */
    private fun isSystemApp(packageName: String): Boolean =
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            true
        }

    /**
     * 计算使用频率
     */
    private fun calculateUsageFrequency(
        stats: UsageStats,
        periodMillis: Long,
    ): UsageFrequency {
        val daysInPeriod = periodMillis / TimeUnit.DAYS.toMillis(1)
        val usageDays = getUsageDaysCount(stats, periodMillis)

        val usageRatio = if (daysInPeriod > 0) usageDays.toFloat() / daysInPeriod.toFloat() else 0f

        return when {
            usageRatio >= 0.7f -> UsageFrequency.VERY_FREQUENTLY
            usageRatio >= 0.4f -> UsageFrequency.FREQUENTLY
            usageRatio >= 0.1f -> UsageFrequency.OCCASIONALLY
            else -> UsageFrequency.RARELY
        }
    }

    /**
     * 获取使用天数
     */
    private fun getUsageDaysCount(
        stats: UsageStats,
        periodMillis: Long,
    ): Int {
        val calendar = Calendar.getInstance()
        val usageDays = mutableSetOf<Int>()

        val startTime = System.currentTimeMillis() - periodMillis
        var currentTime = stats.lastTimeUsed

        while (currentTime >= startTime && usageDays.size < 30) {
            calendar.timeInMillis = currentTime
            usageDays.add(calendar.get(Calendar.DAY_OF_YEAR))
            currentTime -= TimeUnit.DAYS.toMillis(1)
        }

        return usageDays.size
    }

    /**
     * 计算电池影响分数
     */
    private fun calculateBatteryImpactScore(
        stats: UsageStats,
        isSystemApp: Boolean,
    ): Float {
        val hoursInForeground = stats.totalTimeInForeground / (1000f * 60f * 60f)

        val baseScore = hoursInForeground * 10f

        val categoryMultiplier =
            when {
                stats.packageName.contains("com.google.android.youtube") -> 2.0f
                stats.packageName.contains("com.netflix") -> 1.8f
                stats.packageName.contains("com.tencent.mm") -> 1.5f
                stats.packageName.contains("com.facebook") -> 1.6f
                stats.packageName.contains("com.instagram") -> 1.6f
                stats.packageName.contains("com.twitter") -> 1.4f
                stats.packageName.contains("com.tiktok") -> 1.8f
                stats.packageName.contains("com.spotify") -> 1.3f
                stats.packageName.contains("com.whatsapp") -> 1.2f
                stats.packageName.contains("com.google.android.gm") -> 0.8f
                stats.packageName.contains("com.android.chrome") -> 1.1f
                stats.packageName.contains("com.google.android.maps") -> 1.3f
                isSystemApp -> 0.5f
                else -> 1.0f
            }

        return baseScore * categoryMultiplier
    }

    /**
     * 获取使用时间最多的应用
     */
    fun getTopUsedApps(limit: Int = 10): List<AppUsageAnalysis> = analyzeTodayUsage().take(limit)

    /**
     * 获取耗电最多的应用
     */
    fun getTopBatteryDrainingApps(limit: Int = 10): List<AppUsageAnalysis> =
        analyzeTodayUsage()
            .sortedByDescending { it.batteryImpactScore }
            .take(limit)

    /**
     * 获取用户应用
     */
    fun getUserAppsOnly(): List<AppUsageAnalysis> = analyzeTodayUsage().filter { !it.isSystemApp }

    /**
     * 获取系统应用
     */
    fun getSystemAppsOnly(): List<AppUsageAnalysis> = analyzeTodayUsage().filter { it.isSystemApp }

    /**
     * 获取使用情况按类别分组
     */
    fun getUsageByCategory(): Map<String, List<AppUsageAnalysis>> {
        val todayUsage = analyzeTodayUsage()
        val categories = mutableMapOf<String, MutableList<AppUsageAnalysis>>()

        todayUsage.forEach { app ->
            val category = categorizeApp(app.packageName)
            categories.getOrPut(category) { mutableListOf() }.add(app)
        }

        return categories
    }

    /**
     * 获取应用类别
     */
    private fun categorizeApp(packageName: String): String =
        when {
            packageName.contains("com.google.android.youtube") ||
                packageName.contains("com.netflix") ||
                packageName.contains("com.amazon.avod") -> "视频娱乐"

            packageName.contains("com.facebook") ||
                packageName.contains("com.instagram") ||
                packageName.contains("com.twitter") ||
                packageName.contains("com.tiktok") ||
                packageName.contains("com.tencent.mm") ||
                packageName.contains("com.whatsapp") -> "社交网络"

            packageName.contains("com.spotify") ||
                packageName.contains("com.google.android.music") ||
                packageName.contains("com.apple.android.music") -> "音乐音频"

            packageName.contains("com.android.chrome") ||
                packageName.contains("com.mozilla.firefox") ||
                packageName.contains("com.opera.browser") -> "浏览器"

            packageName.contains("com.google.android.gm") ||
                packageName.contains("com.microsoft.office") ||
                packageName.contains("com.adobe.reader") -> "办公效率"

            packageName.contains("com.google.android.maps") ||
                packageName.contains("com.waze") ||
                packageName.contains("com.uber") -> "地图导航"

            packageName.contains("com.google.android.play") ||
                packageName.contains("com.amazon.venezia") -> "应用商店"

            packageName.startsWith("com.android.") ||
                packageName.startsWith("com.google.android.") -> "系统应用"

            else -> "其他应用"
        }

    /**
     * 获取使用情况概要
     */
    fun getUsageSummary(): Map<String, Any> {
        val todayUsage = analyzeTodayUsage()
        val userApps = todayUsage.filter { !it.isSystemApp }
        val systemApps = todayUsage.filter { it.isSystemApp }

        val totalUsageTime = todayUsage.sumOf { it.totalTimeInForeground }
        val totalLaunches = todayUsage.sumOf { it.appLaunchCount }
        val averageBatteryImpact = todayUsage.map { it.batteryImpactScore }.average()

        return mapOf(
            "totalApps" to todayUsage.size,
            "userApps" to userApps.size,
            "systemApps" to systemApps.size,
            "totalUsageTimeMinutes" to (totalUsageTime / (1000 * 60)),
            "totalLaunches" to totalLaunches,
            "averageBatteryImpact" to averageBatteryImpact,
            "mostUsedApp" to (todayUsage.firstOrNull()?.appName ?: "None"),
            "highestBatteryImpact" to (todayUsage.maxByOrNull { it.batteryImpactScore }?.appName ?: "None"),
        )
    }

    companion object {
        /**
         * 判断是否支持应用使用情况分析
         */
        fun isSupported(context: Context): Boolean =
            context.getSystemService(Context.USAGE_STATS_SERVICE) != null
    }
}
