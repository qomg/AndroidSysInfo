package com.example.sysinfo.usage

class DigitalWellbeingMonitor {
    
    fun monitorAppUsage(appPackage: String, dailyLimit: Int): MonitoringResult {
        val todayStats = getTodayUsageStats(appPackage)
        
        return if (todayStats.launchCount > dailyLimit) {
            MonitoringResult(
                status = Status.EXCEEDED,
                currentLaunches = todayStats.launchCount,
                limit = dailyLimit
            )
        } else {
            MonitoringResult(
                status = Status.WITHIN_LIMIT,
                currentLaunches = todayStats.launchCount,
                remaining = dailyLimit - todayStats.launchCount
            )
        }
    }
}