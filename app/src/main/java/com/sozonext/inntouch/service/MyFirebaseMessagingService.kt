package com.sozonext.inntouch.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val intent = Intent(this, PortSipService::class.java)
        intent.setAction(PortSipService.ACTION_PUSH_MESSAGE)
        PortSipService.startServiceCompatibility(this, intent)
    }

    override fun onNewToken(token: String) {
        val intent = Intent(this, PortSipService::class.java)
        intent.setAction(PortSipService.ACTION_PUSH_TOKEN)
        intent.putExtra(PortSipService.EXTRA_PUSH_TOKEN, token)
        PortSipService.startServiceCompatibility(this, intent)
    }

}
