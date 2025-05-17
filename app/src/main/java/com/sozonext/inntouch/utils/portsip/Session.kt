package com.sozonext.inntouch.utils.portsip

class Session {

    companion object {
        const val INVALID_SESSION_ID = -1L
    }

    var sessionId: Long = INVALID_SESSION_ID
    var state: CallStateFlag = CallStateFlag.CLOSED
    var isHold: Boolean = false
    var isMute: Boolean = false

    var remote: String? = null
    var displayName: String? = null
    var bScreenShare: Boolean = false
    var hasVideo: Boolean = false
    var bEarlyMedia: Boolean = false
    var lineName: String? = null

    var targetExtensionName = "xxx"

    fun isIdle(): Boolean {
        return state == CallStateFlag.FAILED || state == CallStateFlag.CLOSED
    }

    fun reset() {

        sessionId = INVALID_SESSION_ID
        state = CallStateFlag.CLOSED

        remote = null
        displayName = null
        hasVideo = false
        bScreenShare = false
        bEarlyMedia = false
    }



}

enum class CallStateFlag {
    INCOMING,   // 着信中
    TRYING,     // 発信中（接続試行中）
    CONNECTED,  // 接続済み
    FAILED,     // 通話失敗
    CLOSED      // 通話終了
}