UsageStatsManager 是 Android 系统中用于获取应用使用统计信息的重要类。它可以帮助你了解用户的使用习惯、应用使用情况以及设备状态。以下是详细的使用指南：

📊 基本介绍

UsageStatsManager 主要用于：
• 查询应用使用历史统计

• 检测应用使用状态

• 监控用户活动

• 获取应用使用时长统计

🔧 权限配置

首先在 AndroidManifest.xml 中添加必要权限：
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />


🚀 基本使用

1. 检查权限并请求授权

public class UsageStatsHelper {
    
    /**
     * 检查是否有使用情况访问权限
     */
    public boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
                                        android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    /**
     * 请求使用情况访问权限
     */
    public void requestUsageStatsPermission(Activity activity, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        activity.startActivityForResult(intent, requestCode);
    }
}


2. 获取 UsageStatsManager 实例

public class UsageStatsManagerHelper {
    private UsageStatsManager mUsageStatsManager;
    private Context mContext;
    
    public UsageStatsManagerHelper(Context context) {
        mContext = context.getApplicationContext();
        mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
    }
}


📈 常用查询方法

1. 查询指定时间范围内的使用统计

public class UsageQueryHelper {
    private UsageStatsManager mUsageStatsManager;
    
    /**
     * 查询今日应用使用情况
     */
    public List<UsageStats> getTodayUsageStats() {
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        
        // 使用 INTERVAL_BEST 自动选择合适的时间间隔
        return mUsageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
    }
    
    /**
     * 查询最近7天的使用统计
     */
    public List<UsageStats> getLast7DaysUsageStats() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (7 * 24 * 60 * 60 * 1000);
        
        return mUsageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime);
    }
}


2. 查询事件统计

public class UsageEventsHelper {
    
    /**
     * 获取使用事件
     */
    public void printRecentUsageEvents(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (1000 * 60 * 60); // 最近1小时
        
        UsageEvents usageEvents = usm.queryEvents(startTime, endTime);
        
        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            
            String eventType = getEventTypeString(event.getEventType());
            String packageName = event.getPackageName();
            
            Log.d("UsageEvents", "事件: " + eventType + ", 包名: " + packageName + 
                  ", 时间: " + new Date(event.getTimeStamp()));
        }
    }
    
    private String getEventTypeString(int eventType) {
        switch (eventType) {
            case UsageEvents.Event.ACTIVITY_RESUMED:
                return "应用回到前台";
            case UsageEvents.Event.ACTIVITY_PAUSED:
                return "应用进入后台";
            case UsageEvents.Event.ACTIVITY_STOPPED:
                return "应用停止";
            case UsageEvents.Event.FOREGROUND_SERVICE_START:
                return "前台服务开始";
            case UsageEvents.Event.FOREGROUND_SERVICE_STOP:
                return "前台服务停止";
            default:
                return "未知事件: " + eventType;
        }
    }
}


🎯 实际应用场景

1. 检测当前前台应用

public class ForegroundAppDetector {
    
    /**
     * 获取当前前台应用包名
     */
    @SuppressLint("NewApi")
    public String getForegroundAppPackage() {
        UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 1000 * 60; // 最近1分钟
        
        UsageEvents usageEvents = usm.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastForegroundApp = null;
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundApp = event.getPackageName();
            }
        }
        
        return lastForegroundApp;
    }
    
    /**
     * 检查特定应用是否在前台
     */
    public boolean isAppInForeground(String targetPackageName) {
        String foregroundApp = getForegroundAppPackage();
        return targetPackageName != null && targetPackageName.equals(foregroundApp);
    }
}


2. 应用使用时长统计

public class AppUsageAnalyzer {
    private UsageStatsManager mUsageStatsManager;
    
    /**
     * 获取今日各应用使用时长
     */
    @SuppressLint("NewApi")
    public Map<String, Long> getTodayAppUsageTime() {
        Map<String, Long> usageTimeMap = new HashMap<>();
        
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        
        List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startOfDay, now);
        
        for (UsageStats usageStats : usageStatsList) {
            if (usageStats.getTotalTimeInForeground() > 0) {
                long minutes = usageStats.getTotalTimeInForeground() / (1000 * 60);
                usageTimeMap.put(usageStats.getPackageName(), minutes);
            }
        }
        
        return usageTimeMap;
    }
    
    /**
     * 获取特定应用今日使用时长（分钟）
     */
    public long getAppUsageTimeToday(String packageName) {
        Map<String, Long> usageMap = getTodayAppUsageTime();
        return usageMap.getOrDefault(packageName, 0L);
    }
}


3. 应用使用频率分析

public class AppUsageFrequency {
    
    /**
     * 分析应用使用频率
     */
    public void analyzeAppUsagePattern(String packageName) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (7 * 24 * 60 * 60 * 1000); // 最近7天
        
        List<UsageStats> weeklyStats = mUsageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime);
        
        for (UsageStats stats : weeklyStats) {
            if (stats.getPackageName().equals(packageName)) {
                long totalTime = stats.getTotalTimeInForeground();
                int launchCount = stats.getAppLaunchCount();
                
                Log.d("UsageAnalysis", "应用: " + packageName +
                      ", 总使用时长: " + (totalTime / 60000) + "分钟" +
                      ", 启动次数: " + launchCount);
                break;
            }
        }
    }
}


🔒 权限处理最佳实践

public class UsageStatsPermissionHelper {
    private static final int REQUEST_CODE_USAGE_STATS = 1001;
    
    /**
     * 完整的权限检查与请求流程
     */
    public void checkAndRequestUsageStatsPermission(Activity activity) {
        if (!hasUsageStatsPermission(activity)) {
            showPermissionDialog(activity);
        } else {
            // 已经有权限，开始使用
            initializeUsageStatsMonitoring();
        }
    }
    
    private void showPermissionDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("需要使用情况访问权限")
               .setMessage("此功能需要访问应用使用统计权限来提供更好的服务")
               .setPositiveButton("去设置", (dialog, which) -> {
                   requestUsageStatsPermission(activity, REQUEST_CODE_USAGE_STATS);
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    /**
     * 在 onActivityResult 中处理权限请求结果
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_USAGE_STATS) {
            if (hasUsageStatsPermission(mContext)) {
                // 用户授予了权限
                initializeUsageStatsMonitoring();
            } else {
                // 用户拒绝了权限
                handlePermissionDenied();
            }
        }
    }
    
    private void initializeUsageStatsMonitoring() {
        // 初始化使用统计监控
        Log.d("UsageStats", "权限已授予，开始监控");
    }
    
    private void handlePermissionDenied() {
        // 处理权限被拒绝的情况
        Log.d("UsageStats", "使用统计权限被拒绝");
    }
}


⚠️ 注意事项

1. API级别：大部分功能需要 API 21 (Android 5.0) 或更高版本
2. 权限要求：必须获得用户明确授权
3. 数据延迟：使用统计数据可能有几分钟的延迟
4. 电池优化：长时间监控可能影响电池寿命
5. 隐私保护：妥善处理用户数据，遵守隐私政策

📋 兼容性处理

public class UsageStatsCompat {
    
    /**
     * 检查设备是否支持 UsageStatsManager
     */
    public static boolean isUsageStatsSupported(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        return usm != null;
    }
    
    /**
     * 兼容性封装的方法
     */
    @SuppressLint("NewApi")
    public List<UsageStats> queryUsageStatsCompat(int intervalType, long startTime, long endTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
            return usm.queryUsageStats(intervalType, startTime, endTime);
        } else {
            return new ArrayList<>();
        }
    }
}


通过合理使用 UsageStatsManager，你可以构建出功能强大的应用使用分析功能，但务必注意用户隐私保护和权限处理的规范性。