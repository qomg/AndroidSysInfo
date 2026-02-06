package com.example.sysinfo.usage

fun getTopLaunchedApps(context: Context, days: Int = 7): List<AppUsageInfo> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) 
        as UsageStatsManager
    
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()
    
    val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
        startTime, 
        endTime
    )
    
    return usageStatsMap.values
        .filter { it.launchCount > 0 }
        .map { usageStats ->
            AppUsageInfo(
                packageName = usageStats.packageName,
                launchCount = usageStats.launchCount,
                totalTimeInForeground = usageStats.totalTimeInForeground,
                lastTimeUsed = usageStats.lastTimeUsed
            )
        }
        .sortedByDescending { it.launchCount }
}

fun getAppUsageStats(context: Context, targetPackage: String): AppUsageDetail? {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) 
        as UsageStatsManager
    
    val endTime = System.currentTimeMillis()
    val startTime = endTime - TimeUnit.DAYS.toMillis(30)  // 最近30天
    
    val usageStatsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_BEST,
        startTime,
        endTime
    )
    
    val targetStats = usageStatsList.find { it.packageName == targetPackage }
    
    return targetStats?.let {
        AppUsageDetail(
            packageName = it.packageName,
            launchCount = it.launchCount,
            totalTimeInForeground = it.totalTimeInForeground,
            firstTimeStamp = it.firstTimeStamp,
            lastTimeStamp = it.lastTimeStamp,
            lastTimeUsed = it.lastTimeUsed
        )
    }
}

data class AppUsageInfo(
    val packageName: String,
    val launchCount: Int,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long
)

data class AppUsageDetail(
    val packageName: String,
    val launchCount: Int,
    val totalTimeInForeground: Long,
    val firstTimeStamp: Long
    val lastTimeStamp: Long
    val lastTimeUsed: Long
)

// 不同的时间间隔会影响统计结果
val intervals = arrayOf(
    UsageStatsManager.INTERVAL_DAILY,      // 天级
    UsageStatsManager.INTERVAL_WEEKLY,     // 周级
    UsageStatsManager.INTERVAL_MONTHLY,    // 月级
    UsageStatsManager.INTERVAL_YEARLY,     // 年级
    UsageStatsManager.INTERVAL_BEST        // 系统决定
)

// 每个间隔的统计是独立的
// 跨间隔统计需要手动汇总

// 某些情况下启动次数可能不准确：
// 1. 应用在后台被杀死后重启
// 2. 系统休眠/重启
// 3. 应用通过深链接直接打开特定页面
// 4. 多用户设备（统计基于当前用户）


// 在测试中模拟使用数据
fun simulateAppUsage(packageName: String, launchTimes: Int) {
    repeat(launchTimes) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        context.startActivity(intent)
        Thread.sleep(1000)  // 短暂使用
        
        // 返回桌面
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        context.startActivity(homeIntent)
    }
}