package com.sozonext.inntouch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.sozonext.inntouch.ui.activity.MainActivity

@RequiresApi(Build.VERSION_CODES.S)
class MyTelephonyCallback(private val context: Context) : TelephonyCallback(), TelephonyCallback.CallStateListener {

    override fun onCallStateChanged(state: Int) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action", "incoming_call")
            }
            context.startActivity(intent)
        }
    }

}