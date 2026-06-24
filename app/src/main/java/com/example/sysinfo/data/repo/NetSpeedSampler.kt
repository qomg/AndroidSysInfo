package com.example.sysinfo.data.repo

import android.net.TrafficStats
import android.os.SystemClock
import com.example.sysinfo.data.model.NetSpeedState

/**
 * 整机实时网速采样器：基于 TrafficStats 累计字节做差值。
 *
 * - 无需任何权限。
 * - 给的是「开机以来」的累计值，速率必须自己按调用间隔算差值。
 * - 非线程安全：约定在同一线程顺序调用（如 ViewModel 的 IO 协程），间隔约 1s。
 */
class NetSpeedSampler {
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTs = 0L

    private val unsupported = TrafficStats.UNSUPPORTED.toLong()

    /** 两次调用的时间差即采样窗口。建议固定 ~1s 调用一次。 */
    fun sample(): NetSpeedState {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        // 设备/ROM 不支持流量统计
        if (rx == unsupported || tx == unsupported) {
            return NetSpeedState(0, 0, 0, 0, supported = false)
        }

        // 首次调用没有前值，只记基线，速率记 0
        if (lastTs == 0L) {
            lastRx = rx; lastTx = tx; lastTs = now
            return NetSpeedState(0, 0, rx, tx)
        }

        val dtMs = (now - lastTs).coerceAtLeast(1)
        val rxSpeed = (rx - lastRx) * 1000 / dtMs
        val txSpeed = (tx - lastTx) * 1000 / dtMs

        lastRx = rx; lastTx = tx; lastTs = now
        return NetSpeedState(
            rxSpeedBps = rxSpeed.coerceAtLeast(0),
            txSpeedBps = txSpeed.coerceAtLeast(0),
            totalRxBytes = rx,
            totalTxBytes = tx,
        )
    }

    /** 重置基线（如页面重新进入想从 0 开始计速时调用）。 */
    fun reset() {
        lastRx = 0L; lastTx = 0L; lastTs = 0L
    }
}
