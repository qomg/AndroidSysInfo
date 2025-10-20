package com.example.sysinfo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sysinfo.SysInfoApp
import com.example.sysinfo.data.model.BatteryState
import com.example.sysinfo.data.model.CellTower
import com.example.sysinfo.data.model.CpuState
import com.example.sysinfo.data.model.HardwareInfo
import com.example.sysinfo.data.model.HotspotState
import com.example.sysinfo.data.model.MemoryState
import com.example.sysinfo.data.model.MobileState
import com.example.sysinfo.data.model.SystemInfo
import com.example.sysinfo.data.model.WifiState
import kotlinx.coroutines.launch

class MonitorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SysInfoApp).repository
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

    init {
        viewModelScope.launch { loadAll() }
    }

    fun refresh() = viewModelScope.launch { loadAll() }

    private suspend fun loadAll() {
        _battery.value = repo.battery()
        _wifi.value = repo.wifi()
        _mobile.value = repo.mobile()
        _hotspot.value = repo.hotspot()
        _cells.value = repo.cellTowers()
        _hardware.value = repo.hardware()
        _memory.value = repo.memory()
        _cpu.value = repo.cpu()
        _system.value = repo.system()
    }
}