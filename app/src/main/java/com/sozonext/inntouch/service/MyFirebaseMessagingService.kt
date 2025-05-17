package com.sozonext.inntouch.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        //super.onMessageReceived(remoteMessage);
        val data: Map<String, String> = remoteMessage.getData()
        if ("call" == data["msg_type"]) {
            val srvIntent: Intent = Intent(this, PortSipService::class.java)
            srvIntent.setAction(PortSipService.ACTION_PUSH_MESSAGE)
            PortSipService.startServiceCompatibility(this, srvIntent)
        }
        if ("im" == data["msg_type"]) {
            val content = data["msg_content"]
            val from = data["send_from"]
            val to = data["send_to"]
            val pushid = data["portsip-push-id"] //old
            val xpushid = data["x-push-id"] //new version
            val srvIntent: Intent = Intent(this, PortSipService::class.java)
            srvIntent.setAction(PortSipService.ACTION_PUSH_MESSAGE)
            PortSipService.startServiceCompatibility(this, srvIntent)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }


    private fun sendRegistrationToServer(token: String) {
        val intent: Intent = Intent(this, PortSipService::class.java)
        intent.setAction(PortSipService.ACTION_PUSH_TOKEN)
        intent.putExtra(PortSipService.EXTRA_PUSH_TOKEN, token)
        PortSipService.startServiceCompatibility(this, intent)
    }
}
