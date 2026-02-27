package com.example.sysinfo.data.repo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.sysinfo.data.model.BatteryState

/**
 * 获取电池温度（兼容所有 API 版本）
 * @return 电池温度（°C），失败返回 -1
 */
fun Context.getBatteryTemperature(): BatteryState {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus: Intent = registerReceiver(null, filter) ?: return BatteryState() // 粘性广播，无需注册

    // 电池温度，单位是 0.1°C 的整数
    val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
    val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) //电池电压（单位：毫伏）
    val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
    val technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "UNKNOWN"
    val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

    val statusStr = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
        BatteryManager.BATTERY_STATUS_FULL -> "已充满"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
        else -> "未知"
    }

    val healthStr = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
        BatteryManager.BATTERY_HEALTH_DEAD -> "没电"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "未知故障"
        BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
        else -> "未知"
    }

    println("电池状态：$statusStr")
    println("电池健康度：$healthStr")
    println("电池电压：${voltage/1000f}伏")

    return BatteryState(
        temperature = temperature / 10.0f, // 转为 °C
        voltage = voltage / 1000f,
        health = health,
        technology = technology,
        status = status
    )
}