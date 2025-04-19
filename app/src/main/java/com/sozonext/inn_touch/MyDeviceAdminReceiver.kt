package com.sozonext.inn_touch

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.sozonext.inn_touch.utils.KioskUtils

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        KioskUtils(context).setLockTaskPackage()
    }

}