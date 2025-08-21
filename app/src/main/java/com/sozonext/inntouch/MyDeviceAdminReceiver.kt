package com.sozonext.inntouch

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sozonext.inntouch.utils.KioskUtil

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    private val tag = this::class.java.simpleName

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(tag, "onEnabled()")
        KioskUtil(context).setLockTaskPackage()
    }

}