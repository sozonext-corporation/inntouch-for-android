package com.sozonext.inntouch.utils

import android.annotation.SuppressLint
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipErrorcode.INVALID_SESSION_ID
import com.portsip.PortSipSdk

class SessionManager {

    companion object {
        const val MAX_LINES = 10

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SessionManager? = null
        fun getInstance(): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }

    }

    private val sessions = Array(MAX_LINES) { Session().apply {} }
    private var currentSessionIndex: Int = 0

    var isRegistered: Boolean = false
    var isOnline: Boolean = false

    private var currentAudioDevice: PortSipEnumDefine.AudioDevice = PortSipEnumDefine.AudioDevice.NONE
    private val availableAudioDevices = mutableSetOf<PortSipEnumDefine.AudioDevice>()

    fun getCurrentAudioDevice(): PortSipEnumDefine.AudioDevice = currentAudioDevice

    fun setCurrentAudioDevice(sdk: PortSipSdk, device: PortSipEnumDefine.AudioDevice) {
        currentAudioDevice = device
        sdk.setAudioDevice(device)
    }

    fun getAvailableAudioDevices(): Set<PortSipEnumDefine.AudioDevice> = availableAudioDevices.toSet()

    fun setAvailableAudioDevices(current: PortSipEnumDefine.AudioDevice, devices: Set<PortSipEnumDefine.AudioDevice>) {
        availableAudioDevices.apply {
            clear()
            addAll(devices)
        }
        currentAudioDevice = current
    }


    fun getCurrentSession(): Session? {
        return if (currentSessionIndex in sessions.indices) sessions[currentSessionIndex] else null
    }

    fun findSessionBySessionId(sessionId: Long): Session? {
        return sessions.find { it.sessionId == sessionId }
    }

    fun findSessionByIndex(index: Int): Session? {
        return if (index in sessions.indices) sessions[index] else null
    }

    fun hasActiveSession(): Boolean {
        return sessions.any { it.sessionId > INVALID_SESSION_ID }
    }

    fun hangUpAllCalls(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session.sessionId > INVALID_SESSION_ID) {
                sdk.hangUp(session.sessionId)
            }
        }
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

    fun resetAll() {
        for (session in sessions) {
            session.reset()
        }
    }

    fun findIncomingCall(): Session? {
        return sessions.find {
            it.sessionId != INVALID_SESSION_ID.toLong() && it.sessionStatus == SessionStatus.INCOMING
        }
    }

}