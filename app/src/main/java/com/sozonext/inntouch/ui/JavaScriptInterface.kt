package com.sozonext.inntouch.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.portsip.PortSipEnumDefine
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.service.PortSipService
import com.sozonext.inntouch.utils.DataStoreUtils
import com.sozonext.inntouch.utils.portsip.CallManager
import com.sozonext.inntouch.utils.portsip.CallStateFlag.CONNECTED
import com.sozonext.inntouch.utils.portsip.CallStateFlag.INCOMING
import com.sozonext.inntouch.utils.portsip.CallStateFlag.TRYING
import com.sozonext.inntouch.utils.portsip.Ring
import com.sozonext.inntouch.utils.portsip.Session
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class JavaScriptInterface(private val context: Context) {

    private val tag: String = context.packageName
    private val portSipSdk = MyApplication.portSipSdk

    @JavascriptInterface
    fun register(sipServer: String, sipDomain: String, extensionNumber: String, extensionPassword: String): Boolean {
        Log.d(tag, "register($sipServer, $sipDomain, $extensionNumber, $extensionPassword)")

        runBlocking {
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.SIP_SERVER, sipServer)
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.SIP_DOMAIN, sipDomain)
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.EXTENSION_NUMBER, extensionNumber)
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.EXTENSION_PASSWORD, extensionPassword)
        }

        val intent = Intent(context, PortSipService::class.java).apply {
            action = PortSipService.ACTION_SIP_REGISTER
        }
        PortSipService.startServiceCompatibility(context, intent)

        return true
    }

    @JavascriptInterface
    fun call(extensionNumber: String): Boolean {

        Log.d(tag, "call($extensionNumber)")

        // Start Tone
        Ring.getInstance(context).startOutgoingTone()

        // TODO
        portSipSdk.clearAudioCodec()
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA);
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729)

        val sessionId = portSipSdk.call(extensionNumber, true, false)
        if (sessionId <= 0) {
            Toast.makeText(context, "Call failure: $sessionId", Toast.LENGTH_SHORT).show()
            return false
        }
        portSipSdk.sendVideo(sessionId, true)
        return true
    }

    @JavascriptInterface
    fun answer(): Boolean {

        Log.d(tag, "answer()")

        val currentSession = CallManager.getInstance().getCurrentSession() ?: return false

        if (currentSession.state !== INCOMING) {
            Toast.makeText(context, "No incoming call on current line, please switch a line.", Toast.LENGTH_SHORT).show()
            return false
        }

        currentSession.state = CONNECTED
        Ring.getInstance(context).stopIncomingTone()

        val result = portSipSdk.answerCall(currentSession.sessionId, false)
        if (result != 0) {
            Toast.makeText(context, "answerCall Failed! Result = $result", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    @JavascriptInterface
    fun hangUp(): Boolean {
        Log.d(tag, "hangUp()")

        val currentSession = CallManager.getInstance().getCurrentSession() ?: return true

        Ring.getInstance(context).stopIncomingTone()
        Ring.getInstance(context).stopOutgoingTone()

        when (currentSession.state) {
            INCOMING -> {
                portSipSdk.rejectCall(currentSession.sessionId, 486)
            }

            CONNECTED, TRYING -> {
                portSipSdk.hangUp(currentSession.sessionId)
            }

            else -> {}
        }

        return true
    }

    @JavascriptInterface
    fun mute(enable: Boolean): Boolean {
        Log.d(tag, "mute($enable)")

        val currentSession = CallManager.getInstance().getCurrentSession() ?: return false
        currentSession.let { session ->
            if (session.state == CONNECTED) {
                currentSession.isHold = enable
                if (enable) {
                    portSipSdk.muteSession(
                        currentSession.sessionId, false, false, false, false
                    )
                } else {
                    portSipSdk.muteSession(
                        currentSession.sessionId, true, true, true, true
                    )
                }
            }
        }
        return true
    }

    @JavascriptInterface
    fun hold(enable: Boolean): Boolean {
        Log.d(tag, "hold($enable)")

        val currentSession = CallManager.getInstance().getCurrentSession() ?: return false
        currentSession.let { session ->
            if (session.state == CONNECTED) {
                currentSession.isHold = enable
                if (enable) {
                    portSipSdk.hold(session.sessionId)
                } else {
                    portSipSdk.unHold(session.sessionId)
                }
            }
        }
        return true

    }

    @JavascriptInterface
    fun getCurrentSession(): String {
        Log.d(tag, "getCurrentSession()")
        val currentSession = CallManager.getInstance().getCurrentSession()
        val json = if (currentSession != null) {
            JSONObject().apply {
                put("targetExtensionName", currentSession.targetExtensionName)
            }.toString()
        } else {
            "{}"
        }
        return json
    }

}