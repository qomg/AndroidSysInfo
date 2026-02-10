package com.example.sysinfo.usage

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