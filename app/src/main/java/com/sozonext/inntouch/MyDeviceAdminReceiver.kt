package com.sozonext.inntouch

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.portsip.PortSipSdk
import com.sozonext.inntouch.utils.KioskUtils

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        KioskUtils(context).setLockTaskPackage()
    }
}