package com.sozonext.inntouch.utils.portsip

import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipSdk

class CallManager {

    companion object {
        private lateinit var mInstance: CallManager
        private val locker: Any = Any()
        fun instance(): CallManager {
            return mInstance
        }
        var isRegistered: Boolean = false
        var isOnline: Boolean = false
    }


}