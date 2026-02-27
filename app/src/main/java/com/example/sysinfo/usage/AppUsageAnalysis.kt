package com.example.sysinfo.usage

/**
 * 应用使用情况分析
 */
data class AppUsageAnalysis(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val appLaunchCount: Int,
    val averageSessionTime: Long,
    val usageFrequency: UsageFrequency,
    val batteryImpactScore: Float,
)