package com.sozonext.inntouch.utils

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import com.sozonext.inntouch.MyDeviceAdminReceiver

class KioskUtil(private val context: Context) {

    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun setLockTaskPackage() {
        dpm.setLockTaskPackages(componentName, arrayOf(context.packageName))
    }

    fun removeDeviceOwner() {
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            dpm.clearDeviceOwnerApp(context.packageName)
        }
    }

    fun start(activity: Activity) {
        if (hasDeviceOwnerPermission()) {
            activity.startLockTask()
            AudioManagerUtil(context).startLockVolume()
            Toast.makeText(context, "アプリを固定しました", Toast.LENGTH_SHORT).show()
        }
    }

    fun stop(activity: Activity) {
        if (hasDeviceOwnerPermission()) {
            activity.stopLockTask()
            AudioManagerUtil(context).stopLockVolume()
            Toast.makeText(context, "アプリの固定を解除しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasDeviceOwnerPermission(): Boolean {
        return dpm.isAdminActive(componentName) && dpm.isDeviceOwnerApp(context.packageName)
    }

}