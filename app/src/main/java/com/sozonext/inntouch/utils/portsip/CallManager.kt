package com.sozonext.inntouch.utils.portsip

import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipSdk

class CallManager() {

    companion object {
        const val MAX_LINES = 10
        private var mInstance: CallManager? = null
        private val locker = Any()

        fun instance(): CallManager {
            if (mInstance == null) {
                synchronized(locker) {
                    if (mInstance == null) {
                        mInstance = CallManager()
                    }
                }
            }
            return mInstance!!
        }
    }

    private var currentLine: Int = 0
    var isRegistered: Boolean = false
    var isOnline: Boolean = false
    var speakerOn: Boolean = false
    private var currentAudioDevice: PortSipEnumDefine.AudioDevice = PortSipEnumDefine.AudioDevice.NONE
    private val audioDeviceAvailable = mutableListOf<PortSipEnumDefine.AudioDevice>()
    private val sessions = Array(MAX_LINES) { Session().apply { lineName = "line - $it" } }

    fun setSelectableAudioDevice(current: PortSipEnumDefine.AudioDevice, devices: Set<PortSipEnumDefine.AudioDevice>) {
        audioDeviceAvailable.clear()
        audioDeviceAvailable.addAll(devices)
        this.currentAudioDevice = current
    }

    fun getSelectableAudioDevice(): Set<PortSipEnumDefine.AudioDevice> {
        return HashSet(audioDeviceAvailable)
    }

    fun setAudioDevice(portSipSdk: PortSipSdk, audioDevice: PortSipEnumDefine.AudioDevice) {
        currentAudioDevice = audioDevice
        portSipSdk.setAudioDevice(currentAudioDevice)
    }

    fun getCurrentAudioDevice(): PortSipEnumDefine.AudioDevice {
        return currentAudioDevice
    }

    fun hangupAllCalls(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session.sessionID > Session.INVALID_SESSION_ID) {
                sdk.hangUp(session.sessionID)
            }
        }
    }

    fun hasActiveSession(): Boolean {
        return sessions.any { it.sessionID > Session.INVALID_SESSION_ID }
    }

    fun findSessionBySessionID(sessionID: Long): Session? {
        return sessions.find { it.sessionID == sessionID }
    }

    fun findIdleSession(): Session? {
        for (session in sessions) {
            if (session.isIdle()) {
                session.reset()
                return session
            }
        }
        return null
    }

    fun getCurrentSession(): Session? {
        return if (currentLine in sessions.indices) sessions[currentLine] else null
    }

    fun findSessionByIndex(index: Int): Session? {
        return if (index in sessions.indices) sessions[index] else null
    }

    fun addActiveSessionToConference(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session.state == Session.CallStateFlag.CONNECTED) {
                sdk.setRemoteScreenWindow(session.sessionID, null)
                sdk.setRemoteVideoWindow(session.sessionID, null)
                sdk.joinToConference(session.sessionID)
                sdk.sendVideo(session.sessionID, true)
                sdk.unHold(session.sessionID)
            }
        }
    }

    fun setRemoteVideoWindow(sdk: PortSipSdk, sessionId: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session.state == Session.CallStateFlag.CONNECTED && session.sessionID != sessionId) {
                sdk.setRemoteVideoWindow(session.sessionID, null)
            }
        }
        sdk.setRemoteVideoWindow(sessionId, renderer)
    }

    fun setShareVideoWindow(sdk: PortSipSdk, sessionId: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session.state == Session.CallStateFlag.CONNECTED && session.sessionID != sessionId) {
                sdk.setRemoteScreenWindow(session.sessionID, null)
            }
        }
        sdk.setRemoteScreenWindow(sessionId, renderer)
    }

    fun setConferenceVideoWindow(sdk: PortSipSdk, renderer: PortSIPVideoRenderer?) {
        for (session in sessions) {
            if (session.state == Session.CallStateFlag.CONNECTED) {
                sdk.setRemoteVideoWindow(session.sessionID, null)
                sdk.setRemoteScreenWindow(session.sessionID, null)
            }
        }
        sdk.setConferenceVideoWindow(renderer)
    }

    fun resetAll() {
        for (session in sessions) {
            session.reset()
        }
    }

    fun findIncomingCall(): Session? {
        return sessions.find {
            it.sessionID != Session.INVALID_SESSION_ID && it.state == Session.CallStateFlag.INCOMING
        }
    }

}