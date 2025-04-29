package com.sozonext.inntouch.utils.portsip

class Session {

    companion object {
        const val INVALID_SESSION_ID = -1L
    }

    var sessionID: Long = INVALID_SESSION_ID
    var remote: String? = null
    var displayName: String? = null
    var bScreenShare: Boolean = false
    var hasVideo: Boolean = false
    var bHold: Boolean = false
    var bMute: Boolean = false
    var bEarlyMedia: Boolean = false
    var lineName: String? = null
    var state: CallStateFlag = CallStateFlag.CLOSED

    fun isIdle(): Boolean {
        return state == CallStateFlag.FAILED || state == CallStateFlag.CLOSED
    }

    fun reset() {
        remote = null
        displayName = null
        hasVideo = false
        bScreenShare = false
        sessionID = INVALID_SESSION_ID
        state = CallStateFlag.CLOSED
        bEarlyMedia = false
    }

    enum class CallStateFlag {
        INCOMING,
        TRYING,
        CONNECTED,
        FAILED,
        CLOSED
    }

}