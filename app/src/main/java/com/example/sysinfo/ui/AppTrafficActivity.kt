package com.example.sysinfo.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sysinfo.data.model.AppNetUsage
import com.example.sysinfo.databinding.ActivityAppTrafficBinding
import com.example.sysinfo.usage.NetworkUsageAnalyzer
import com.example.sysinfo.usage.UsageStatsPermissionHelper
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分 App 流量列表页：按 全部 / WiFi / 移动 切换排序，点击某行跳系统应用详情。
 */
class AppTrafficActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppTrafficBinding
    private val adapter = AppTrafficAdapter(::openAppDetails)
    private val analyzer by lazy { NetworkUsageAnalyzer(this) }

    /** 全量数据，切 Tab 时在内存里重排，不必重查。 */
    private var allApps: List<AppNetUsage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppTrafficBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "应用流量 (今日)"

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = applyTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // 从设置页授权回来后会重新加载
        loadData()
    }

    private fun loadData() {
        if (!UsageStatsPermissionHelper.hasUsageStatsPermission(this)) {
            showEmpty("需开启「使用情况访问」权限才能统计应用流量（点击前往设置）")
            binding.empty.setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            return
        }
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { analyzer.analyzeTodayUsage() }
            allApps = data
            if (data.isEmpty()) {
                showEmpty("暂无流量数据")
            } else {
                binding.empty.visibility = android.view.View.GONE
                binding.recycler.visibility = android.view.View.VISIBLE
                applyTab(binding.tabs.selectedTabPosition.coerceAtLeast(0))
            }
        }
    }

    /** 0=全部 1=WiFi 2=移动 */
    private fun applyTab(position: Int) {
        val list = when (position) {
            1 -> allApps.filter { it.wifiBytes > 0 }.sortedByDescending { it.wifiBytes }
            2 -> allApps.filter { it.mobileBytes > 0 }.sortedByDescending { it.mobileBytes }
            else -> allApps.filter { it.totalBytes > 0 }.sortedByDescending { it.totalBytes }
        }
        adapter.submitList(list)
    }

    private fun showEmpty(msg: String) {
        binding.empty.text = msg
        binding.empty.visibility = android.view.View.VISIBLE
        binding.recycler.visibility = android.view.View.GONE
    }

    /** 跳到系统「应用详情」页；非真实包（系统聚合 uid）则提示。 */
    private fun openAppDetails(app: AppNetUsage) {
        val pkg = app.packageName
        if (pkg.startsWith("uid:") || !pkg.contains('.')) {
            Toast.makeText(this, "${app.appName}：系统聚合流量，无对应应用页", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", pkg, null))
            )
        } catch (_: Exception) {
            Toast.makeText(this, "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }
}
