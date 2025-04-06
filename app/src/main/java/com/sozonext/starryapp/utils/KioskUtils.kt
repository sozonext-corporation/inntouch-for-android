package com.sozonext.starryapp.utils

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import com.sozonext.starryapp.MyDeviceAdminReceiver

class KioskUtils(private val context: Context) {

    private val deviceAdmin = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun setLockTaskPackage() {
        dpm.setLockTaskPackages(deviceAdmin, arrayOf(context.packageName))
    }

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
        return dpm.isAdminActive(deviceAdmin) && dpm.isDeviceOwnerApp(context.packageName)
    }

}