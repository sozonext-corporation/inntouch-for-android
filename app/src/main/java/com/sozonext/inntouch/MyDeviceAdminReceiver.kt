package com.sozonext.inntouch

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.sozonext.inntouch.utils.KioskUtil

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        KioskUtil(context).setLockTaskPackage()
    }

}