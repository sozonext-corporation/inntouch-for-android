package com.sozonext.inntouch.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.portsip.PortSipEnumDefine
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.service.PortSipService
import com.sozonext.inntouch.ui.activity.VideoCallActivity
import com.sozonext.inntouch.utils.DataStoreUtil
import com.sozonext.inntouch.utils.Ring
import com.sozonext.inntouch.utils.Session
import com.sozonext.inntouch.utils.SessionManager
import com.sozonext.inntouch.utils.SessionStatus.CONNECTED
import com.sozonext.inntouch.utils.SessionStatus.INCOMING
import com.sozonext.inntouch.utils.SessionStatus.TRYING
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class JavaScriptInterface(private val context: Context) {

    private val tag: String = context.packageName
    private val portSipSdk = MyApplication.portSipSdk

    @JavascriptInterface
    fun register(sipServer: String, sipDomain: String, extensionNumber: String, extensionPassword: String): Boolean {
        Log.d(tag, "register($sipServer, $sipDomain, $extensionNumber, $extensionPassword)")

        runBlocking {
            DataStoreUtil(context).setDataStoreValue(DataStoreUtil.SIP_SERVER, sipServer)
            DataStoreUtil(context).setDataStoreValue(DataStoreUtil.SIP_DOMAIN, sipDomain)
            DataStoreUtil(context).setDataStoreValue(DataStoreUtil.EXTENSION_NUMBER, extensionNumber)
            DataStoreUtil(context).setDataStoreValue(DataStoreUtil.EXTENSION_PASSWORD, extensionPassword)
        }

        val intent = Intent(context, PortSipService::class.java).apply {
            action = PortSipService.ACTION_SIP_REGISTER
        }
        PortSipService.startServiceCompatible(context, intent)

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

        val session: Session = SessionManager.getInstance().getCurrentSession() ?: return false

        val extensionDisplayName = ""

        session.sessionStatus = TRYING
        session.sessionId = sessionId
        session.targetExtensionNumber = extensionNumber
        session.targetExtensionDisplayName = extensionDisplayName
        return true
    }

    @JavascriptInterface
    fun videoCall(extensionNumber: String): Boolean {
        Log.d(tag, "videoCall($extensionNumber)")

        val intent = Intent(context, VideoCallActivity::class.java).apply {
            putExtra("extensionNumber", extensionNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    @JavascriptInterface
    fun answer(): Boolean {
        Log.d(tag, "answer()")

        Ring.getInstance(context).stopIncomingTone()

        val currentSession = SessionManager.getInstance().getCurrentSession() ?: return false

        if (currentSession.sessionStatus !== INCOMING) {
            return false
        }

        currentSession.sessionStatus = CONNECTED

        val result = portSipSdk.answerCall(currentSession.sessionId, false)
        if (result != 0) {
            Toast.makeText(context, "answerCall Failed! Result = $result", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    @JavascriptInterface
    fun hangUp() {
        Log.d(tag, "hangUp()")

        // Stop Tone
        Ring.getInstance(context).stopIncomingTone()
        Ring.getInstance(context).stopOutgoingTone()

        val currentSession = SessionManager.getInstance().getCurrentSession() ?: return
        when (currentSession.sessionStatus) {
            INCOMING -> {
                portSipSdk.rejectCall(currentSession.sessionId, 486)
            }

            CONNECTED, TRYING -> {
                portSipSdk.hangUp(currentSession.sessionId)
            }

            else -> {}
        }
        currentSession.reset()
    }

    @JavascriptInterface
    fun mute(enable: Boolean): Boolean {
        Log.d(tag, "mute($enable)")

        val currentSession = SessionManager.getInstance().getCurrentSession() ?: return false
        currentSession.let { session ->
            if (session.sessionStatus == CONNECTED) {
                currentSession.isMute = enable
                if (enable) {
                    portSipSdk.muteSession(
                        currentSession.sessionId, true, true, true, true
                    )
                } else {
                    portSipSdk.muteSession(
                        currentSession.sessionId, false, false, false, false
                    )
                }
            }
        }
        return true
    }

    @JavascriptInterface
    fun hold(enable: Boolean): Boolean {
        Log.d(tag, "hold($enable)")

        val currentSession = SessionManager.getInstance().getCurrentSession() ?: return false
        currentSession.let { session ->
            if (session.sessionStatus == CONNECTED) {
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

        val currentSession = SessionManager.getInstance().getCurrentSession()
        val json = if (currentSession != null) {
            JSONObject().apply {
                put("targetExtensionDisplayName", currentSession.targetExtensionDisplayName)
            }.toString()
        } else {
            "{}"
        }
        return json
    }

    @JavascriptInterface
    fun getBatteryChargingStatus(): Boolean {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, filter)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    @JavascriptInterface
    fun getUsbAttachingStatus(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        return deviceList.isNotEmpty()
    }

    @JavascriptInterface
    fun getBatteryLevel(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent: Intent? = context.registerReceiver(null, filter)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    @JavascriptInterface
    fun getWifiLevel(): Int {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return -1
        val network = connectivityManager.activeNetwork ?: return -1
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return -1
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return -1

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return -1

        @Suppress("DEPRECATION")
        val rssi = wifiManager.connectionInfo.rssi

        return when {
            rssi >= -50 -> 4
            rssi >= -60 -> 3
            rssi >= -70 -> 2
            rssi >= -80 -> 1
            else -> 0
        }
    }

}