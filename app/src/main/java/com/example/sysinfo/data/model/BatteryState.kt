package com.example.sysinfo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class BatteryState(
    val level: Int = -1,
    val temperature: Float = -1f,
    val voltage: Float = -1f,
    val status: Int = -1,
    val health: Int = -1,
    val technology: String = "UNKNOWN",
    val isCharging: Boolean = false,
    val isPowerSave: Boolean = false
)

data class WifiState(
    val isConnected: Boolean, val ssid: String?, val bssid: String?,
    val rssi: Int, val linkSpeedMbps: Int, val frequencyMhz: Int
)

data class MobileState(
    val isConnected: Boolean, val networkType: String, val operator: String,
    val roaming: Boolean, val cellInfoJson: String // CellInfo 序列化保留原始字段
)

data class HotspotState(val isEnabled: Boolean, val clients: List<String> = emptyList())

@Entity
data class CellTower(
    val mcc: String, val mnc: String, val lac: String, val cid: String,
    val type: String, val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(true)
    val id: Long = 0,
)

data class HardwareInfo(
    val brand: String, val model: String, val manufacturer: String,
    val hardware: String, val device: String, val product: String,
    val board: String, val bootloader: String, val fingerprint: String,
    val serial: String?, val androidId: String
)

data class MemoryState(
    val totalBytes: Long, 
    val availableBytes: Long, 
    val thresholdBytes: Long,
    val lowMemory: Boolean,
    // 获取普通内存类
    val normalHeap: Int,
    // 获取大内存类（如果设置了 largeHeap）
    val largeHeap: Int,
)

data class CpuState(
    val cores: Int, val freqMaxKhz: Long, val freqMinKhz: Long, val freqCurKhz: Long,
    val arch: String, val supportedAbis: List<String>
)

data class SystemInfo(
    val os: String, val sdkInt: Int, val buildId: String, val securityPatch: String?
)

/**
 * 整机实时网速（基于 TrafficStats 采样差值）
 */
data class NetSpeedState(
    val rxSpeedBps: Long,    // 下行速率 B/s
    val txSpeedBps: Long,    // 上行速率 B/s
    val totalRxBytes: Long,  // 开机以来累计下行
    val totalTxBytes: Long,  // 开机以来累计上行
    val supported: Boolean = true,
)

/**
 * 单个应用的流量统计（基于 NetworkStatsManager），按 WiFi / 移动网络分项。
 */
data class AppNetUsage(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val wifiRx: Long = 0,    // WiFi 下行
    val wifiTx: Long = 0,    // WiFi 上行
    val mobileRx: Long = 0,  // 移动 下行
    val mobileTx: Long = 0,  // 移动 上行
) {
    val wifiBytes: Long get() = wifiRx + wifiTx
    val mobileBytes: Long get() = mobileRx + mobileTx
    val rxBytes: Long get() = wifiRx + mobileRx
    val txBytes: Long get() = wifiTx + mobileTx
    val totalBytes: Long get() = rxBytes + txBytes
}