package com.sozonext.inntouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log

class BatteryStatusReceiver(private val batteryStatusListener: BatteryStatusListener) : BroadcastReceiver() {

    private val tag = this::class.java.simpleName

    interface BatteryStatusListener {
        fun onBatteryStatusChanged(isCharging: Boolean)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "onReceive()")
        intent?.let {
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryStatusListener.onBatteryStatusChanged(isCharging)
        }
    }
}