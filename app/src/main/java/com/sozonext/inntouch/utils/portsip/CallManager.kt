package com.sozonext.inntouch.utils.portsip

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.Call
import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipSdk

class CallManager() {

    companion object {
        const val MAX_LINES = 10

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: CallManager? = null
        fun getInstance(): CallManager =
            instance ?: synchronized(this) {
                instance ?: CallManager().also { instance = it }
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
            if (session.sessionId > Session.INVALID_SESSION_ID) {
                sdk.hangUp(session.sessionId)
            }
        }
    }

    fun hasActiveSession(): Boolean {
        return sessions.any { it.sessionId > Session.INVALID_SESSION_ID }
    }

    fun findSessionBySessionId(sessionId: Long): Session? {
        return sessions.find { it.sessionId == sessionId }
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
            if (session.state == CallStateFlag.CONNECTED) {
                sdk.setRemoteScreenWindow(session.sessionId, null)
                sdk.setRemoteVideoWindow(session.sessionId, null)
                sdk.joinToConference(session.sessionId)
                sdk.sendVideo(session.sessionId, true)
                sdk.unHold(session.sessionId)
            }
        }
    }

    fun setRemoteVideoWindow(sdk: PortSipSdk, sessionId: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session.state == CallStateFlag.CONNECTED && session.sessionId != sessionId) {
                sdk.setRemoteVideoWindow(session.sessionId, null)
            }
        }
        sdk.setRemoteVideoWindow(sessionId, renderer)
    }

    fun setShareVideoWindow(sdk: PortSipSdk, sessionId: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session.state == CallStateFlag.CONNECTED && session.sessionId != sessionId) {
                sdk.setRemoteScreenWindow(session.sessionId, null)
            }
        }
        sdk.setRemoteScreenWindow(sessionId, renderer)
    }

    fun setConferenceVideoWindow(sdk: PortSipSdk, renderer: PortSIPVideoRenderer?) {
        for (session in sessions) {
            if (session.state == CallStateFlag.CONNECTED) {
                sdk.setRemoteVideoWindow(session.sessionId, null)
                sdk.setRemoteScreenWindow(session.sessionId, null)
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
            it.sessionId != Session.INVALID_SESSION_ID && it.state == CallStateFlag.INCOMING
        }
    }

}