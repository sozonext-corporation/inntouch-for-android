package com.sozonext.starryapp

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.sozonext.starryapp.utils.KioskUtils

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        KioskUtils(context).setLockTaskPackage()
    }

}