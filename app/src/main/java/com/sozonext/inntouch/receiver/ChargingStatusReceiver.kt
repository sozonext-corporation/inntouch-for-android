package com.sozonext.inntouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

class BatteryStatusReceiver(private val batteryStatusListener: BatteryStatusListener) : BroadcastReceiver() {

    interface BatteryStatusListener {
        fun onBatteryStatusChanged(isCharging: Boolean)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryStatusListener.onBatteryStatusChanged(isCharging)
        }
    }
}