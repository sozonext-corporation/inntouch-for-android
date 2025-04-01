package com.sozonext.starryapp

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class KioskUtils(private val context: Context) {

    private val deviceAdmin = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun setLockTaskPackage() {
        dpm.setLockTaskPackages(deviceAdmin, arrayOf(context.packageName))
    }

    fun start(activity: Activity) {
        if (hasDeviceOwnerPermission()) {
            activity.startLockTask()
        }
    }

    fun stop(activity: Activity) {
        if (hasDeviceOwnerPermission()) {
            activity.stopLockTask()
        }
    }

    private fun hasDeviceOwnerPermission(): Boolean {
        return dpm.isAdminActive(deviceAdmin) && dpm.isDeviceOwnerApp(context.packageName)
    }

}