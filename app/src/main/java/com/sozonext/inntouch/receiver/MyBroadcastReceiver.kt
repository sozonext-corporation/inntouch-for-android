package com.sozonext.inntouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.sozonext.inntouch.ui.activity.MainActivity

class MyBroadcastReceiver : BroadcastReceiver() {

    private val tag = this::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(tag, "onReceive()")
        if (intent?.action == ACTION_BOOT_COMPLETED) {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(this)
            }
        }
    }

}