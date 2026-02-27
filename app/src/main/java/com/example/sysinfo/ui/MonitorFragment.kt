package com.example.sysinfo.ui

import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.sysinfo.R
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
import com.example.sysinfo.databinding.FragMonitorBinding
import com.example.sysinfo.service.MonitorService

class MonitorFragment : Fragment(R.layout.frag_monitor) {
    private val vm: MonitorViewModel by activityViewModels()
    private lateinit var binding: FragMonitorBinding

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        binding = FragMonitorBinding.bind(v)
        (requireActivity().application as SysInfoApp).repository // 触发初始化
        setupRefresh()
        observe()

        binding.btnStartService.setOnClickListener {
            val intent = Intent(requireContext(), MonitorService::class.java)
            requireContext().startForegroundService(intent)
            Toast.makeText(requireContext(), "后台监测服务已启动", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRefresh() {
        binding.swipe.setOnRefreshListener { vm.refresh() }
    }

    private fun observe() {
        vm.battery.observe(viewLifecycleOwner) { b -> bindBattery(b) }
        vm.wifi.observe(viewLifecycleOwner) { w -> bindWifi(w) }
        vm.mobile.observe(viewLifecycleOwner) { m -> bindMobile(m) }
        vm.hotspot.observe(viewLifecycleOwner) { h -> bindHotspot(h) }
        vm.cells.observe(viewLifecycleOwner) { cs -> bindCells(cs) }
        vm.hardware.observe(viewLifecycleOwner) { h -> bindHardware(h) }
        vm.memory.observe(viewLifecycleOwner) { m -> bindMemory(m) }
        vm.cpu.observe(viewLifecycleOwner) { c -> bindCpu(c) }
        vm.system.observe(viewLifecycleOwner) { s -> bindSystem(s) }
    }

    private fun bindBattery(b: BatteryState) {
        binding.apply {
            battery.text = "电量: ${b.level}%"
            temp.text = "温度: ${"%.1f".format(b.temperature)}°C"
            volt.text = "电压: ${"%.2f".format(b.voltage)}V"
            status.text = "状态: ${statusStr(b.status)}"; powerSave.text =
            "省电: ${if (b.isPowerSave) "开" else "关"}"
        }
    }

    private fun bindWifi(w: WifiState?) {
        binding.wifi.text = w?.run { "Wi‑Fi: $ssid (${if (isConnected) "已连接" else "未连接"})" }
            ?: "Wi‑Fi: 不可用"
        binding.rssi.text = w?.let { "RSSI: ${it.rssi}dBm / ${it.frequencyMhz}MHz" } ?: ""
    }

    private fun bindMobile(m: MobileState) {
        binding.mobile.text =
            "移动: ${m.networkType} (${m.operator})${if (m.roaming) " 漫游" else ""}"
        binding.roaming.text = "漫游: ${if (m.isConnected) "是" else "否"}"
    }

    private fun bindHotspot(h: HotspotState) {
        binding.hotspot.text = "热点: ${if (h.isEnabled) "开" else "关"}"
    }

    private fun bindCells(cs: List<CellTower>) {
        binding.cells.text =
            "小区(${cs.size}): ${cs.take(2).joinToString { "${it.mcc}-${it.mnc}-${it.cid}" }}..."
    }

    private fun bindHardware(h: HardwareInfo) {
        binding.device.text = "${h.manufacturer} ${h.model} (${h.device})"
        binding.board.text = "主板: ${h.board}"
    }

    private fun bindMemory(m: MemoryState) {
        binding.memory.text = "内存: ${fmt(m.availableBytes)}/${fmt(m.totalBytes)}"
    }

    private fun bindCpu(c: CpuState) {
        binding.cpu.text = "CPU: ${c.cores}核 ${c.freqCurKhz / 1_000}MHz"
    }

    private fun bindSystem(s: SystemInfo) {
        binding.os.text = "系统: ${s.os} (SDK ${s.sdkInt})"
    }

    private fun statusStr(s: Int): String {
        return when (s) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            else -> "未知"
        }
    }

    private fun fmt(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp.toDouble()), pre)
    }



}