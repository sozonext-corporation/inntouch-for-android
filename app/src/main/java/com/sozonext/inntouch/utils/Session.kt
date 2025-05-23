package com.sozonext.inntouch.utils

import com.portsip.PortSipErrorcode.INVALID_SESSION_ID

class Session {

    var sessionId: Long = INVALID_SESSION_ID.toLong()
    var sessionStatus: SessionStatus = SessionStatus.CLOSED

    var targetExtensionNumber: String = ""
    var targetExtensionDisplayName: String = ""
    var isHold: Boolean = false
    var isMute: Boolean = false

    var existsEarlyMedia: Boolean = false

    fun isIdle(): Boolean {
        return sessionStatus == SessionStatus.FAILED || sessionStatus == SessionStatus.CLOSED
    }

    fun reset() {
        sessionId = INVALID_SESSION_ID.toLong()
        sessionStatus = SessionStatus.CLOSED

        targetExtensionNumber = ""
        targetExtensionDisplayName = ""
        isHold = false
        isMute = false

        existsEarlyMedia = false
    }

}
