package com.example.sysinfo.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sysinfo.R
import com.example.sysinfo.data.model.AppNetUsage
import com.example.sysinfo.utils.ByteFormat

/**
 * 分 App 流量列表适配器。点击某行回调 [onClick]（用于跳系统应用详情页）。
 */
class AppTrafficAdapter(
    private val onClick: (AppNetUsage) -> Unit,
) : ListAdapter<AppNetUsage, AppTrafficAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_traffic, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)
        private val name: TextView = itemView.findViewById(R.id.appName)
        private val wifiLine: TextView = itemView.findViewById(R.id.wifiLine)
        private val mobileLine: TextView = itemView.findViewById(R.id.mobileLine)
        private val total: TextView = itemView.findViewById(R.id.totalBytes)

        fun bind(item: AppNetUsage) {
            val pm = itemView.context.packageManager
            icon.setImageDrawable(loadIcon(pm, item.packageName))
            name.text = item.appName
            wifiLine.text =
                "WiFi  ↓${ByteFormat.bytes(item.wifiRx)}  ↑${ByteFormat.bytes(item.wifiTx)}"
            mobileLine.text =
                "移动  ↓${ByteFormat.bytes(item.mobileRx)}  ↑${ByteFormat.bytes(item.mobileTx)}"
            total.text = ByteFormat.bytes(item.totalBytes)
            itemView.setOnClickListener { onClick(item) }
        }

        private fun loadIcon(pm: PackageManager, pkg: String) =
            try {
                pm.getApplicationIcon(pkg)
            } catch (_: PackageManager.NameNotFoundException) {
                itemView.context.getDrawable(android.R.drawable.sym_def_app_icon)
            }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppNetUsage>() {
            override fun areItemsTheSame(a: AppNetUsage, b: AppNetUsage) = a.uid == b.uid
            override fun areContentsTheSame(a: AppNetUsage, b: AppNetUsage) = a == b
        }
    }
}
