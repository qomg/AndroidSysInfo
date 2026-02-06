package com.example.sysinfo.data.repo

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.telephony.*
import androidx.core.content.ContextCompat
import com.example.sysinfo.data.model.BatteryState
import com.example.sysinfo.data.model.CellTower
import com.example.sysinfo.data.model.CpuState
import com.example.sysinfo.data.model.HardwareInfo
import com.example.sysinfo.data.model.HotspotState
import com.example.sysinfo.data.model.MemoryState
import com.example.sysinfo.data.model.MobileState
import com.example.sysinfo.data.model.SystemInfo
import com.example.sysinfo.data.model.WifiState
import java.io.File

class SysInfoRepository(private val ctx: Context) {
    private val power = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun battery(): BatteryState = ctx.getSystemService(BatteryManager::class.java).run {
        val status = getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val capacity = getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ctx.getBatteryTemperature().copy(level = capacity, status = status, isCharging = isCharging, isPowerSave = power.isPowerSaveMode)
    }

    fun wifi(): WifiState? {
        if (!hasPerm(Manifest.permission.ACCESS_WIFI_STATE)) return null
        val info = wm.connectionInfo ?: return null
        return WifiState(
            isConnected = cm.getNetworkCapabilities(cm.activeNetwork)
                ?.hasTransport(TRANSPORT_WIFI) == true,
            ssid = info.ssid?.takeIf { it != "<unknown ssid>" },
            bssid = info.bssid,
            rssi = info.rssi, linkSpeedMbps = info.linkSpeed,
            frequencyMhz = info.frequency
        )
    }

    @SuppressLint("MissingPermission")
    fun mobile(): MobileState {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val connected = caps?.hasTransport(TRANSPORT_CELLULAR) == true
        val op = tm.networkOperator
        val type = when (tm.networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"; TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> tm.networkType.toString()
        }
        return MobileState(
            isConnected = connected,
            networkType = type,
            operator = if (op.isEmpty()) {
                "Unknown"
            } else {
                "${op.substring(0, 3)}-${op.substring(3, 5)}"
            },
            roaming = tm.isNetworkRoaming,
            cellInfoJson = runCatching {
                tm.allCellInfo.joinToString { it.toString() }
            }.getOrDefault("[]")
        )
    }

    fun hotspot(): HotspotState {
        if (!hasPerm(Manifest.permission.ACCESS_WIFI_STATE)) return HotspotState(false)
        val enabled = try {
            val wifiManager = Class.forName("android.net.wifi.WifiManager")
            val isWifiApEnabled = wifiManager.getMethod("isWifiApEnabled").invoke(wm) as Boolean
            isWifiApEnabled
        } catch (_: Exception) {
            false
        }
        return HotspotState(enabled)
    }

    @SuppressLint("MissingPermission")
    fun cellTowers(): List<CellTower> {
        if (!hasPerm(Manifest.permission.ACCESS_FINE_LOCATION)) return emptyList()
        return tm.allCellInfo?.mapNotNull {
            when (it) {
                is CellInfoWcdma -> {
                    val identity = it.cellIdentity
                    CellTower(
                        type = "WCDMA",
                        mcc = runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                identity.mccString
                            } else {
                                identity.mcc.toString()
                            }
                        }.getOrNull() ?: "N/A",
                        mnc = runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                identity.mncString
                            } else {
                                identity.mnc.toString()
                            }
                        }.getOrNull() ?: "N/A",
                        lac = identity.lac.toString(),
                        cid = identity.cid.toString(),
                    )
                }

                is CellInfoGsm -> {
                    val identity = it.cellIdentity
                    CellTower(
                        lac = identity.lac.toString(),
                        cid = identity.cid.toString(),
                        mcc = "",
                        mnc = "",
                        type = "GSM",
                    )
                }

                is CellInfoLte -> {
                    val tac = it.cellIdentity.tac
                    val ci = it.cellIdentity.ci
                    CellTower(
                        lac = tac.toString(),
                        cid = ci.toString(),
                        mcc = "",
                        mnc = "",
                        type = "LTE"
                    )
                }

                else -> {
                    // 5G
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it is CellInfoNr) {
                        val identity = it.cellIdentity as CellIdentityNr
                        // 如果你只想展示一个唯一标识，可以用 mcc-mnc-ci 或 mcc-mnc-tac-cid
                        CellTower(
                            mcc = identity.mccString ?: "N/A",
                            mnc = identity.mncString ?: "N/A",
                            lac = "N/A",  // NR 通常没有 LAC，用 N/A 或空
                            cid = identity.nci.toString(),
                            type = "NR (tac=${identity.tac},pci=${identity.pci})",
                            timestamp = System.currentTimeMillis()
                        )
                    } else {
                        null
                    }
                }
            }
        } ?: emptyList()
    }

    @SuppressLint("HardwareIds")
    fun hardware(): HardwareInfo = HardwareInfo(
        brand = Build.BRAND,
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        hardware = Build.HARDWARE,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        board = Build.BOARD,
        bootloader = Build.BOOTLOADER,
        fingerprint = Build.FINGERPRINT,
        serial = if (hasPerm(Manifest.permission.READ_PHONE_STATE)) Build.SERIAL else null,
        androidId = android.provider.Settings.Secure.getString(
            ctx.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    )

    fun memory(): MemoryState {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return MemoryState(mi.totalMem, mi.availMem, mi.threshold, mi.lowMemory)
    }

    fun cpu(): CpuState {
        val cores = File("/sys/devices/system/cpu/").listFiles { _, name ->
            name.startsWith("cpu") && Regex("cpu\\d+").matches(name)
        }?.size ?: 1
        val freqMax =
            readFirstLine("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")?.toLongOrNull()
                ?.times(1000) ?: 0L
        val freqMin =
            readFirstLine("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")?.toLongOrNull()
                ?.times(1000) ?: 0L
        val freqCur =
            readFirstLine("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")?.toLongOrNull()
                ?.times(1000) ?: 0L
        return CpuState(
            cores = cores, freqMaxKhz = freqMax, freqMinKhz = freqMin, freqCurKhz = freqCur,
            arch = System.getProperty("os.arch", "unknown") ?: "unknown",
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        )
    }

    fun system(): SystemInfo = SystemInfo(
        os = "Android ${Build.VERSION.RELEASE}",
        sdkInt = Build.VERSION.SDK_INT,
        buildId = Build.DISPLAY,
        securityPatch = Build.VERSION.SECURITY_PATCH
    )

    private fun hasPerm(perm: String) =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    private fun readFirstLine(path: String): String? = try {
        File(path).bufferedReader().useLines { it.firstOrNull() }
    } catch (_: Exception) {
        null
    }
}