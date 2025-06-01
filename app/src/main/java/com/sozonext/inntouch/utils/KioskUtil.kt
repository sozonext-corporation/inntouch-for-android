package com.sozonext.inntouch.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import com.sozonext.inntouch.MyDeviceAdminReceiver

class KioskUtil(private val context: Context) {

    private val dar = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun setLockTaskPackage() {
        dpm.setLockTaskPackages(dar, arrayOf(context.packageName))
    }

    @SuppressLint("ServiceCast")
    fun start(activity: Activity) {
        if (hasDeviceOwnerPermission()) {
            activity.startLockTask()
            Toast.makeText(context, "アプリを固定しました", Toast.LENGTH_SHORT).show()
        }
    }

    fun stop(activity: Activity) {
        if (hasDeviceOwnerPermission()) {
            activity.stopLockTask()
            Toast.makeText(context, "アプリの固定を解除しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasDeviceOwnerPermission(): Boolean {
        return dpm.isAdminActive(dar) && dpm.isDeviceOwnerApp(context.packageName)
    }

}