package com.sozonext.inntouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PortSipBroadcastReceiver : BroadcastReceiver() {

    private val tag = this::class.java.simpleName

    private lateinit var broadcastReceiver: BroadcastListener

    interface BroadcastListener {
        fun onBroadcastReceiver(intent: Intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive()")
        broadcastReceiver.onBroadcastReceiver(intent)
    }

}