package com.example.sysinfo.usage

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import com.example.sysinfo.data.model.AppNetUsage
import java.util.concurrent.TimeUnit

/**
 * 基于 NetworkStatsManager 的分 App 流量统计。
 *
 * 需要「使用情况访问」权限（PACKAGE_USAGE_STATS），授权检测/引导复用
 * [UsageStatsPermissionHelper]。无权限时各方法返回空列表，不会抛异常。
 */
class NetworkUsageAnalyzer(private val context: Context) {

    private val nsm: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    private val packageManager: PackageManager by lazy { context.packageManager }

    /** 今日各 App 流量，按总流量降序。 */
    fun analyzeTodayUsage(): List<AppNetUsage> = analyzeForPeriod(TimeUnit.DAYS.toMillis(1))

    /** 最近 7 天各 App 流量。 */
    fun analyzeLast7DaysUsage(): List<AppNetUsage> = analyzeForPeriod(TimeUnit.DAYS.toMillis(7))

    /** 流量最高的前 N 个 App。 */
    fun getTopApps(limit: Int = 10): List<AppNetUsage> = analyzeTodayUsage().take(limit)

    private fun analyzeForPeriod(periodMillis: Long): List<AppNetUsage> {
        val manager = nsm ?: return emptyList()

        val endTime = System.currentTimeMillis()
        val startTime = endTime - periodMillis

        // uid -> [wifiRx, wifiTx, mobileRx, mobileTx]
        // 同一 uid 会有多个 bucket（不同 state/tag），必须累加
        val acc = HashMap<Int, LongArray>()

        // WiFi 与移动网络分开查询，分别落到各自分项；subscriberId 现代写法统一传 null
        for (networkType in intArrayOf(
            ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_MOBILE,
        )) {
            val rxIdx = if (networkType == ConnectivityManager.TYPE_WIFI) 0 else 2
            val stats = try {
                manager.querySummary(networkType, null, startTime, endTime)
            } catch (_: Exception) {
                null
            } ?: continue

            val bucket = NetworkStats.Bucket()
            try {
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val arr = acc.getOrPut(bucket.uid) { LongArray(4) }
                    arr[rxIdx] += bucket.rxBytes
                    arr[rxIdx + 1] += bucket.txBytes
                }
            } finally {
                stats.close()
            }
        }

        return acc.map { (uid, v) ->
            AppNetUsage(
                uid = uid,
                packageName = resolvePackage(uid),
                appName = resolveLabel(uid),
                wifiRx = v[0], wifiTx = v[1],
                mobileRx = v[2], mobileTx = v[3],
            )
        }.sortedByDescending { it.totalBytes }
    }

    /** uid 反查包名；系统聚合 uid 给个可读名称。 */
    private fun resolvePackage(uid: Int): String =
        packageManager.getNameForUid(uid) ?: when (uid) {
            Process.SYSTEM_UID -> "android.system"
            0 -> "root"
            else -> "uid:$uid"
        }

    /** uid 反查 App 显示名（取该 uid 下第一个包）。 */
    private fun resolveLabel(uid: Int): String {
        val pkgs = packageManager.getPackagesForUid(uid)
        if (pkgs.isNullOrEmpty()) return resolvePackage(uid)
        return try {
            val info = packageManager.getApplicationInfo(pkgs[0], 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkgs[0]
        }
    }

    companion object {
        fun isSupported(context: Context): Boolean =
            context.getSystemService(Context.NETWORK_STATS_SERVICE) != null
    }
}
