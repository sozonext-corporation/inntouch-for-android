package com.sozonext.inntouch.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.portsip.OnPortSIPEvent
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_UDP
import com.portsip.PortSipErrorcode
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.receiver.NetWorkReceiver
import com.sozonext.inntouch.utils.DataStoreUtils
import com.sozonext.inntouch.utils.portsip.CallManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Random

class PortSipService : Service(), OnPortSIPEvent, NetWorkReceiver.NetWorkListener {

    private val tag: String = PortSipService::class.java.simpleName
    private val portSipSdk = MyApplication.instance.portSipSdk
    private var pushToken: String = ""

    companion object {

        private const val APP_ID = "com.sozonext.inntouch";

        const val ACTION_SIP_REGISTER: String = "com.sozonext.inntouch.action.SIP_REGISTER"
        const val ACTION_SIP_UNREGISTER: String = "com.sozonext.inntouch.action.SIP_UNREGISTER"

        const val ACTION_PUSH_TOKEN: String = "com.sozonext.inntouch.action.PUSH_TOKEN"
        const val ACTION_PUSH_MESSAGE: String = "com.sozonext.inntouch.action.PUSH_MESSAGE"

        const val ACTION_SIP_AUDIO_DEVICE_UPDATE: String = "com.sozonext.inntouch.action.ACTION_SIP_AUDIO_DEVICE_UPDATE"

        const val INSTANCE_ID: String = "instanceId"
        const val USER_NAME: String = "user name"
        const val USER_PWD: String = "user pwd"
        const val SVR_HOST: String = "svr host"
        const val SVR_PORT: String = "svr port"
        const val USER_DOMAIN: String = "user domain"
        const val USER_DISPLAY_NAME: String = "user dispalay"
        const val USER_AUTH_NAME: String = "user auth name"
        const val STUN_HOST: String = "stun host"
        const val STUN_PORT: String = "stun port"
        const val TRANS: String = "trans type"
        const val SRTP: String = "srtp type"
        const val REGISTER_CHANGE_ACTION: String = "PortSip.AndroidSample.Test.RegisterStatusChange"
        const val CALL_CHANGE_ACTION: String = "PortSip.AndroidSample.Test.CallStatusChange"
        const val PRESENCE_CHANGE_ACTION: String = "PortSip.AndroidSample.Test.PRESENCEStatusChange"
        const val EXTRA_REGISTER_STATE: String = "RegisterStatus"
        const val EXTRA_CALL_SESSION_ID: String = "SessionID"
        const val EXTRA_CALL_DESCRIPTION: String = "Description"
        const val EXTRA_PUSH_TOKEN: String = "token"

        fun startServiceCompatibility(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        when (intent.action) {
            // ACTION_SIP_REGISTER
            ACTION_SIP_REGISTER -> {
                if (!CallManager.instance().isOnline) {
                    initial()
                    register()
                }
            }
            // ACTION_SIP_UNREGISTER
            ACTION_SIP_UNREGISTER -> {
                if (CallManager.instance().isOnline) {
                    unregister()
                }
            }
            // ACTION_PUSH_TOKEN
            ACTION_PUSH_TOKEN -> {
            }
            // ACTION_PUSH_MESSAGE
            ACTION_PUSH_MESSAGE -> {
            }
        }
        return result
    }

    private fun initial(): Int {
        portSipSdk.setOnPortSIPEvent(this)

        CallManager.instance().isOnline = true

        val externalFilesPath = getExternalFilesDir(null)!!.absolutePath
        val tlsCertificatesRootPath = "$externalFilesPath/certs"
        val localSIPPort = 5060 + Random().nextInt(60000)

        var result = portSipSdk.initialize(
            ENUM_TRANSPORT_UDP, "0.0.0.0", localSIPPort, PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, externalFilesPath, 8, "PortSIP SDK for Android", 0, 0, tlsCertificatesRootPath, "", false, null
        )
        if (result != PortSipErrorcode.ECoreErrorNone) {
            CallManager.instance().resetAll()
        } else {
            result = portSipSdk.setLicenseKey("LicenseKey")
            if (result == PortSipErrorcode.ECoreWrongLicenseKey) {
                log("The wrong license key was detected, please check with sales@portsip.com or support@portsip.com")
            } else if (result == PortSipErrorcode.ECoreTrialVersionLicenseKey) {
                log("This Is Trial Version")
                // portSipSdk.setInstanceId(getInstanceID())
            }
        }
        return result
    }

    private fun register() {

        val sipServer: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.SIP_SERVER).first().toString()
        }
        val sipDomain: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.SIP_DOMAIN).first().toString()
        }
        val exceptionNumber: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.EXTENSION_NUMBER).first().toString()
        }
        val exceptionPassword: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.EXTENSION_PASSWORD).first().toString()
        }

        portSipSdk.removeUser()
        var result = portSipSdk.setUser(
            exceptionNumber,
            exceptionNumber,
            exceptionNumber,
            exceptionPassword,
            sipDomain,
            sipServer,
            5060,
            "",
            3478,
            null,
            5060
        )

        if (result != PortSipErrorcode.ECoreErrorNone) {
            log("setUser failure ErrorCode = $result")
            CallManager.instance().resetAll()
            return
        }

        portSipSdk.enableAudioManager(true)

        portSipSdk.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)
        portSipSdk.setVideoDeviceId(1)

        portSipSdk.setSrtpPolicy(0)
        // PortSipService.ConfigPreferences(this, preferences, portSipSdk)

        portSipSdk.enable3GppTags(false)

        if (!TextUtils.isEmpty(pushToken)) {
            val headerValue = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$APP_ID"
            portSipSdk.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", headerValue)
            portSipSdk.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", headerValue)
        }

        result = portSipSdk.registerServer(90, 0)
        if (result != PortSipErrorcode.ECoreErrorNone) {
            log("registerServer failure ErrorCode =$result")
            portSipSdk.unRegisterServer(100)
            CallManager.instance().resetAll()
        }
    }

    private fun unregister() {
        if (CallManager.instance().isOnline) {
            portSipSdk.unRegisterServer(100)
            portSipSdk.removeUser()
            portSipSdk.unInitialize()
            CallManager.instance().isOnline = false
            CallManager.instance().isRegistered = false
        }
    }

    override fun onBind(p0: Intent): IBinder? {
        Log.d(tag, "onInviteIncoming: $p0")
        log("onInviteIncoming: $p0")
        return null
    }

    override fun onRegisterSuccess(p0: String, p1: Int, p2: String) {
        Log.d(tag, "onRegisterSuccess: $p0, $p1, $p2")
        log("onRegisterSuccess: $p0, $p1, $p2")
    }

    override fun onRegisterFailure(p0: String, p1: Int, p2: String) {
        Log.d(tag, "onRegisterFailure: $p0, $p1, $p2")
        log("onRegisterFailure: $p0, $p1, $p2")
    }

    override fun onInviteIncoming(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        log("onInviteIncoming: $p0, $p1, $p2, $p3, $p4, $p5, $p6, $p7, $p8, $p9")
    }

    override fun onInviteTrying(p0: Long) {
        log("onInviteTrying: $p0")
    }

    override fun onInviteSessionProgress(p0: Long, p1: String, p2: String, p3: Boolean, p4: Boolean, p5: Boolean, p6: String) {
        log("onInviteSessionProgress: $p0, $p1, $p2, $p3, $p4, $p5, $p6")
    }

    override fun onInviteRinging(p0: Long, p1: String, p2: Int, p3: String) {
        log("onInviteRinging: $p0, $p1, $p2, $p3")
    }

    override fun onInviteAnswered(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        log("onInviteAnswered: $p0, $p1, $p2, $p3, $p4, $p5, $p6, $p7, $p8, $p9")
    }

    override fun onInviteFailure(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {
        log("onInviteFailure: $p0, $p1, $p2, $p3, $p4, $p5, $p6, $p7")
    }

    override fun onInviteUpdated(p0: Long, p1: String, p2: String, p3: String, p4: Boolean, p5: Boolean, p6: Boolean, p7: String) {
        log("onInviteUpdated: $p0, $p1, $p2, $p3, $p4, $p5, $p6, $p7")
    }

    override fun onInviteConnected(p0: Long) {
        log("onInviteConnected: $p0")
    }

    override fun onInviteBeginingForward(p0: String) {
        log("onInviteBeginingForward: $p0")
    }

    override fun onInviteClosed(p0: Long, p1: String) {
        log("onInviteClosed: $p0, $p1")
    }

    override fun onDialogStateUpdated(p0: String, p1: String, p2: String, p3: String) {
        log("onDialogStateUpdated: $p0, $p1, $p2, $p3")
    }

    override fun onRemoteHold(p0: Long) {
        log("onRemoteHold: $p0")
    }

    override fun onRemoteUnHold(p0: Long, p1: String, p2: String, p3: Boolean, p4: Boolean) {
        log("onRemoteUnHold: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onReceivedRefer(p0: Long, p1: Long, p2: String, p3: String, p4: String) {
        log("onReceivedRefer: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onReferAccepted(p0: Long) {
        log("onReferAccepted: $p0")
    }

    override fun onReferRejected(p0: Long, p1: String, p2: Int) {
        log("onReferRejected: $p0, $p1, $p2")
    }

    override fun onTransferTrying(p0: Long) {
        log("onTransferTrying: $p0")
    }

    override fun onTransferRinging(p0: Long) {
        log("onTransferRinging: $p0")
    }

    override fun onACTVTransferSuccess(p0: Long) {
        log("onACTVTransferSuccess: $p0")
    }

    override fun onACTVTransferFailure(p0: Long, p1: String, p2: Int) {
        log("onACTVTransferFailure: $p0, $p1, $p2")
    }

    override fun onReceivedSignaling(p0: Long, p1: String) {
        log("onReceivedSignaling: $p0, $p1")
    }

    override fun onSendingSignaling(p0: Long, p1: String) {
        log("onSendingSignaling: $p0, $p1")
    }

    override fun onWaitingVoiceMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {
        log("onWaitingVoiceMessage: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onWaitingFaxMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {
        log("onWaitingFaxMessage: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onRecvDtmfTone(p0: Long, p1: Int) {
        log("onRecvDtmfTone: $p0, $p1")
    }

    override fun onRecvOptions(p0: String) {
        log("onRecvOptions: $p0")
    }

    override fun onRecvInfo(p0: String) {
        log("onRecvInfo: $p0")
    }

    override fun onRecvNotifyOfSubscription(p0: Long, p1: String, p2: ByteArray, p3: Int) {
        log("onRecvNotifyOfSubscription: $p0, $p1, $p2, $p3")
    }

    override fun onPresenceRecvSubscribe(p0: Long, p1: String, p2: String, p3: String) {
        log("onPresenceRecvSubscribe: $p0, $p1, $p2, $p3")
    }

    override fun onPresenceOnline(p0: String, p1: String, p2: String) {
        log("onPresenceOnline: $p0, $p1, $p2")
    }

    override fun onPresenceOffline(p0: String, p1: String) {
        log("onPresenceOffline: $p0, $p1")
    }

    override fun onRecvMessage(p0: Long, p1: String, p2: String, p3: ByteArray, p4: Int) {
        log("onRecvMessage: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onRecvOutOfDialogMessage(p0: String, p1: String, p2: String, p3: String, p4: String, p5: String, p6: ByteArray, p7: Int, p8: String) {
        log("onRecvOutOfDialogMessage: $p0, $p1, $p2, $p3, $p4, $p5, $p6, $p7, $p8")
    }

    override fun onSendMessageSuccess(p0: Long, p1: Long, p2: String) {
        log("onSendMessageSuccess: $p0, $p1, $p2")
    }

    override fun onSendMessageFailure(p0: Long, p1: Long, p2: String, p3: Int, p4: String) {
        log("onSendMessageFailure: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onSendOutOfDialogMessageSuccess(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String) {
        log("onSendOutOfDialogMessageSuccess: $p0, $p1, $p2, $p3, $p4, $p5")
    }

    override fun onSendOutOfDialogMessageFailure(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {
        log("onSendOutOfDialogMessageFailure: $p0, $p1, $p2, $p3, $p4, $p5, $p6, $p7")
    }

    override fun onSubscriptionFailure(p0: Long, p1: Int) {
        log("onSubscriptionFailure: $p0, $p1")
    }

    override fun onSubscriptionTerminated(p0: Long) {
        log("onSubscriptionTerminated: $p0")
    }

    override fun onPlayFileFinished(p0: Long, p1: String) {
        log("onPlayFileFinished: $p0, $p1")
    }

    override fun onStatistics(p0: Long, p1: String) {
        log("onStatistics: $p0, $p1")
    }

    override fun onAudioDeviceChanged(p0: PortSipEnumDefine.AudioDevice, p1: MutableSet<PortSipEnumDefine.AudioDevice>) {
        log("onAudioDeviceChanged: $p0, $p1")
    }

    override fun onAudioFocusChange(p0: Int) {
        log("onAudioFocusChange: $p0")
    }

    override fun onRTPPacketCallback(p0: Long, p1: Int, p2: Int, p3: ByteArray, p4: Int) {
        log("onRTPPacketCallback: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onAudioRawCallback(p0: Long, p1: Int, p2: ByteArray, p3: Int, p4: Int) {
        log("onAudioRawCallback: $p0, $p1, $p2, $p3, $p4")
    }

    override fun onVideoRawCallback(p0: Long, p1: Int, p2: Int, p3: Int, p4: ByteArray, p5: Int) {
        log("onVideoRawCallback: $p0, $p1, $p2, $p3, $p4, $p5")
    }

    override fun onNetworkChange(netMobile: Int) {
        log("onNetworkChange: $netMobile")
    }

    private fun log(message: String) {
        Log.d(tag, message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}