package com.example.sysinfo.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class UsageStatsPermissionHelper(
    private val fragment: Fragment,
) {
    companion object {
        private const val TAG = "UsageStatsPermission"
        private const val REQUEST_CODE_USAGE_STATS = 1001

        fun hasUsageStatsPermission(context: Context): Boolean {

            val appOps =
                context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                    ?: return false

            val mode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        context.packageName,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        context.packageName,
                    )
                }

            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun isUsageStatsSupported(): Boolean = true
    }

    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun checkAndRequestPermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit,
    ) {
        val context = fragment.requireContext()

        if (!isUsageStatsSupported()) {
            Log.w(TAG, "UsageStatsManager not supported on this device")
            onPermissionDenied()
            return
        }

        if (hasUsageStatsPermission(context)) {
            Log.d(TAG, "Usage stats permission already granted")
            onPermissionGranted()
        } else {
            permissionCallback = { granted ->
                if (granted) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        val context = fragment.requireContext()

        AlertDialog
            .Builder(context)
            .setTitle("需要使用情况访问权限")
            .setMessage("此功能需要访问应用使用统计权限来分析电池用量和提供更好的服务。\n\n请在设置中授予此权限。")
            .setPositiveButton("去设置") { _, _ ->
                requestUsageStatsPermission()
            }.setNegativeButton("取消") { _, _ ->
                permissionCallback?.invoke(false)
            }.setCancelable(false)
            .show()
    }

    private fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            fragment.startActivityForResult(intent, REQUEST_CODE_USAGE_STATS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open usage access settings", e)
            permissionCallback?.invoke(false)
        }
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (requestCode == REQUEST_CODE_USAGE_STATS) {
            val context = fragment.requireContext()
            val hasPermission = hasUsageStatsPermission(context)

            Log.d(TAG, "Permission request result: granted=$hasPermission")
            permissionCallback?.invoke(hasPermission)
            permissionCallback = null
        }
    }

    fun requestPermissionWithLauncher(
        launcher: ActivityResultLauncher<Intent>,
        onResult: (Boolean) -> Unit,
    ) {
        val context = fragment.requireContext()

        if (hasUsageStatsPermission(context)) {
            onResult(true)
            return
        }

        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            launcher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch permission request", e)
            onResult(false)
        }
    }
}

class UsageStatsPermissionManager(
    private val context: Context,
) {
    fun checkPermissionStatus(): PermissionStatus {
        if (!UsageStatsPermissionHelper.isUsageStatsSupported()) {
            return PermissionStatus.NOT_SUPPORTED
        }

        return if (UsageStatsPermissionHelper.hasUsageStatsPermission(context)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    fun getPermissionExplanation(): String =
        when (checkPermissionStatus()) {
            PermissionStatus.NOT_SUPPORTED -> "您的设备不支持应用使用统计功能（需要Android 5.0或更高版本）。"
            PermissionStatus.GRANTED -> "已获得使用情况访问权限，可以正常使用电池用量统计功能。"
            PermissionStatus.DENIED -> "需要使用情况访问权限才能分析电池用量。此权限允许应用查看其他应用的使用统计信息。"
        }

    fun createPermissionIntent(): Intent? =
        try {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        } catch (e: Exception) {
            Log.e("PermissionManager", "Failed to create permission intent", e)
            null
        }

    enum class PermissionStatus {
        GRANTED,
        DENIED,
        NOT_SUPPORTED,
    }
}
