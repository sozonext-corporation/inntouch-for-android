package com.sozonext.inntouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PortSipBroadcastReceiver : BroadcastReceiver() {

    private lateinit var broadcastReceiver: BroadcastListener

    interface BroadcastListener {
        fun onBroadcastReceiver(intent: Intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        broadcastReceiver.onBroadcastReceiver(intent)
    }

}