package com.sozonext.inntouch.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val PUSH_MSG_TYPE = "msg_type";
        private const val PUSH_MSG_TYPE_CALL: String = "call"
        private const val PUSH_MSG_TYPE_CALL_AUDIO: String = "audio"
        private const val PUSH_MSG_TYPE_CALL_VIDEO: String = "video"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // super.onMessageReceived(remoteMessage);
        val msgType = remoteMessage.data[PUSH_MSG_TYPE]
        if (PUSH_MSG_TYPE_CALL == msgType || PUSH_MSG_TYPE_CALL_AUDIO == msgType || PUSH_MSG_TYPE_CALL_VIDEO == msgType) {
            val intent = Intent(this, PortSipService::class.java as Class<*>)
            intent.setAction(PortSipService.ACTION_PUSH_MESSAGE)
            PortSipService.startServiceCompatible(this, intent)
        }
    }

    override fun onNewToken(token: String) {
        val intent = Intent(this, PortSipService::class.java)
        intent.setAction(PortSipService.ACTION_PUSH_TOKEN)
        intent.putExtra(PortSipService.EXTRA_PUSH_TOKEN, token)
        PortSipService.startServiceCompatible(this, intent)
    }

}
