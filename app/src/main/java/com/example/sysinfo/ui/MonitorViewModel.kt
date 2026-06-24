package com.example.sysinfo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sysinfo.SysInfoApp
import com.example.sysinfo.data.model.AppNetUsage
import com.example.sysinfo.data.model.BatteryState
import com.example.sysinfo.data.model.CellTower
import com.example.sysinfo.data.model.CpuState
import com.example.sysinfo.data.model.HardwareInfo
import com.example.sysinfo.data.model.HotspotState
import com.example.sysinfo.data.model.MemoryState
import com.example.sysinfo.data.model.MobileState
import com.example.sysinfo.data.model.NetSpeedState
import com.example.sysinfo.data.model.SystemInfo
import com.example.sysinfo.data.model.WifiState
import com.example.sysinfo.data.repo.NetSpeedSampler
import com.example.sysinfo.usage.NetworkUsageAnalyzer
import com.example.sysinfo.usage.UsageStatsPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SysInfoApp).repository
    private val netSpeedSampler = NetSpeedSampler()
    private val netUsageAnalyzer = NetworkUsageAnalyzer(app)
    private val _battery = MutableLiveData<BatteryState>()
    val battery: LiveData<BatteryState> = _battery
    private val _wifi = MutableLiveData<WifiState?>()
    val wifi: LiveData<WifiState?> = _wifi
    private val _mobile = MutableLiveData<MobileState>()
    val mobile: LiveData<MobileState> = _mobile
    private val _hotspot = MutableLiveData<HotspotState>()
    val hotspot: LiveData<HotspotState> = _hotspot
    private val _cells = MutableLiveData<List<CellTower>>()
    val cells: LiveData<List<CellTower>> = _cells
    private val _hardware = MutableLiveData<HardwareInfo>()
    val hardware: LiveData<HardwareInfo> = _hardware
    private val _memory = MutableLiveData<MemoryState>()
    val memory: LiveData<MemoryState> = _memory
    private val _cpu = MutableLiveData<CpuState>()
    val cpu: LiveData<CpuState> = _cpu
    private val _system = MutableLiveData<SystemInfo>()
    val system: LiveData<SystemInfo> = _system

    // Feature 1: 整机实时网速（每秒刷新）
    private val _netSpeed = MutableLiveData<NetSpeedState>()
    val netSpeed: LiveData<NetSpeedState> = _netSpeed

    // Feature 2: 分 App 流量（今日，按总流量降序，取前 N）
    private val _appNet = MutableLiveData<List<AppNetUsage>>()
    val appNet: LiveData<List<AppNetUsage>> = _appNet

    init {
        viewModelScope.launch { loadAll() }
        startNetSpeedTicker()
    }

    fun refresh() = viewModelScope.launch { loadAll() }

    private suspend fun loadAll() = withContext(Dispatchers.IO) {
        _battery.postValue(repo.battery())
        _wifi.postValue(repo.wifi())
        _mobile.postValue(repo.mobile())
        _hotspot.postValue(repo.hotspot())
        _cells.postValue(repo.cellTowers())
        _hardware.postValue(repo.hardware())
        _memory.postValue(repo.memory())
        _cpu.postValue(repo.cpu())
        _system.postValue(repo.system())
        // 分 App 流量需要「使用情况访问」权限，没授权就给空列表
        val apps = if (UsageStatsPermissionHelper.hasUsageStatsPermission(getApplication())) {
            netUsageAnalyzer.getTopApps(8)
        } else {
            emptyList()
        }
        _appNet.postValue(apps)
    }

    /** 每秒采样一次整机网速；viewModelScope 在 onCleared 时自动取消。 */
    private fun startNetSpeedTicker() = viewModelScope.launch {
        while (isActive) {
            val speed = withContext(Dispatchers.IO) { netSpeedSampler.sample() }
            _netSpeed.value = speed
            delay(1000)
        }
    }
}