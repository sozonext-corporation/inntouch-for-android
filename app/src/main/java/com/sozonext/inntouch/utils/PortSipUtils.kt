package com.sozonext.inntouch.utils

import com.portsip.PortSipSdk
import com.sozonext.inntouch.ui.activity.MainActivity

class PortSipUtils(private val context: MainActivity) {

    companion object {
        const val EVENT = "event"
        const val EVENT_REGISTER = "register"
        const val EVENT_CALL = "call"
        const val EVENT_ANSWER = "answer"
        const val EVENT_HANG_UP = "hangUp"
        const val EVENT_MUTE = "mute"
    }

    private val sdk = PortSipSdk(context)

    fun register () {

    }

    fun call () {

    }

    fun answer () {

    }

    fun hangUp () {

    }

    fun mute () {

    }

}