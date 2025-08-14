package com.sozonext.inntouch.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.sozonext.inntouch.application.MyApplication

class NotificationUtils(private val context: Context) {

    private val portSip = MyApplication.portSipSdk

    fun refreshPushToken(token: String) {
        val appId = context.packageName;
        val pushMessage = "device-os=android;device-uid=$token;allow-call-push=true;allow-message-push=true;app-id=$appId"
        portSip.addSipMessageHeader(-1, "REGISTER", 1, "X-Push", pushMessage)
        portSip.refreshRegistration(0)
    }

}