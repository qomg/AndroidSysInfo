package com.example.sysinfo.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.example.sysinfo.R

class PermissionFragment : Fragment(R.layout.frag_permission) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btnRequestPermission).setOnClickListener {
            requestPermissions(it.context)
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermissions(requireContext())
    }

    private fun requestPermissions(context: Context) {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        val notGranted = perms.filter {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            requestPermissions(notGranted.toTypedArray(), 1001)
        } else {
            showInfo()
        }
    }

    private fun showInfo() {
        parentFragmentManager.commitNow {
            replace(id, MonitorFragment())
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    requireActivity(),
                    "部分权限未授予，部分信息可能无法显示",
                    Toast.LENGTH_LONG
                ).show()
            }
            showInfo()
        }
    }
}