package com.sozonext.inntouch.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.portsip.OnPortSIPEvent
import com.portsip.PortSipEnumDefine
import com.sozonext.inntouch.receiver.NetWorkReceiver
import com.sozonext.inntouch.ui.activity.MainActivity
import com.sozonext.inntouch.utils.portsip.CallManager

class PortSipService : Service(), OnPortSIPEvent, NetWorkReceiver.NetWorkListener {

    companion object {
        const val ACTION_SIP_REGISTER: String = "PortSip.AndroidSample.Test.REGISTER"
        const val ACTION_SIP_UNREGISTER: String = "PortSip.AndroidSample.Test.UNREGISTER"
        const val ACTION_SIP_AUDIODEVICE: String = "PortSip.AndroidSample.Test.AudioDeviceUpdate"
        const val ACTION_PUSH_MESSAGE: String = "PortSip.AndroidSample.Test.PushMessageIncoming"
        const val ACTION_PUSH_TOKEN: String = "PortSip.AndroidSample.Test.PushToken"
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

    override fun onBind(p0: Intent): IBinder {
        TODO("Not yet implemented")
    }

    override fun onRegisterSuccess(p0: String, p1: Int, p2: String) {
        // CallManager.instance().isRegistered = true
        val intent: Intent = Intent(REGISTER_CHANGE_ACTION)
    }

    override fun onRegisterFailure(p0: String, p1: Int, p2: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteIncoming(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteTrying(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onInviteSessionProgress(p0: Long, p1: String, p2: String, p3: Boolean, p4: Boolean, p5: Boolean, p6: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteRinging(p0: Long, p1: String, p2: Int, p3: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteAnswered(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteFailure(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteUpdated(p0: Long, p1: String, p2: String, p3: String, p4: Boolean, p5: Boolean, p6: Boolean, p7: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteConnected(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onInviteBeginingForward(p0: String) {
        TODO("Not yet implemented")
    }

    override fun onInviteClosed(p0: Long, p1: String) {
        TODO("Not yet implemented")
    }

    override fun onDialogStateUpdated(p0: String, p1: String, p2: String, p3: String) {
        TODO("Not yet implemented")
    }

    override fun onRemoteHold(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onRemoteUnHold(p0: Long, p1: String, p2: String, p3: Boolean, p4: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onReceivedRefer(p0: Long, p1: Long, p2: String, p3: String, p4: String) {
        TODO("Not yet implemented")
    }

    override fun onReferAccepted(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onReferRejected(p0: Long, p1: String, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun onTransferTrying(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onTransferRinging(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onACTVTransferSuccess(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onACTVTransferFailure(p0: Long, p1: String, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun onReceivedSignaling(p0: Long, p1: String) {
        TODO("Not yet implemented")
    }

    override fun onSendingSignaling(p0: Long, p1: String) {
        TODO("Not yet implemented")
    }

    override fun onWaitingVoiceMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onWaitingFaxMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onRecvDtmfTone(p0: Long, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onRecvOptions(p0: String) {
        TODO("Not yet implemented")
    }

    override fun onRecvInfo(p0: String) {
        TODO("Not yet implemented")
    }

    override fun onRecvNotifyOfSubscription(p0: Long, p1: String, p2: ByteArray, p3: Int) {
        TODO("Not yet implemented")
    }

    override fun onPresenceRecvSubscribe(p0: Long, p1: String, p2: String, p3: String) {
        TODO("Not yet implemented")
    }

    override fun onPresenceOnline(p0: String, p1: String, p2: String) {
        TODO("Not yet implemented")
    }

    override fun onPresenceOffline(p0: String, p1: String) {
        TODO("Not yet implemented")
    }

    override fun onRecvMessage(p0: Long, p1: String, p2: String, p3: ByteArray, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onRecvOutOfDialogMessage(p0: String, p1: String, p2: String, p3: String, p4: String, p5: String, p6: ByteArray, p7: Int, p8: String) {
        TODO("Not yet implemented")
    }

    override fun onSendMessageSuccess(p0: Long, p1: Long, p2: String) {
        TODO("Not yet implemented")
    }

    override fun onSendMessageFailure(p0: Long, p1: Long, p2: String, p3: Int, p4: String) {
        TODO("Not yet implemented")
    }

    override fun onSendOutOfDialogMessageSuccess(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String) {
        TODO("Not yet implemented")
    }

    override fun onSendOutOfDialogMessageFailure(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {
        TODO("Not yet implemented")
    }

    override fun onSubscriptionFailure(p0: Long, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onSubscriptionTerminated(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onPlayFileFinished(p0: Long, p1: String) {
        TODO("Not yet implemented")
    }

    override fun onStatistics(p0: Long, p1: String) {
        TODO("Not yet implemented")
    }

    override fun onAudioDeviceChanged(p0: PortSipEnumDefine.AudioDevice, p1: MutableSet<PortSipEnumDefine.AudioDevice>) {
        TODO("Not yet implemented")
    }

    override fun onAudioFocusChange(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onRTPPacketCallback(p0: Long, p1: Int, p2: Int, p3: ByteArray, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onAudioRawCallback(p0: Long, p1: Int, p2: ByteArray, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }

    override fun onVideoRawCallback(p0: Long, p1: Int, p2: Int, p3: Int, p4: ByteArray, p5: Int) {
        TODO("Not yet implemented")
    }

    override fun onNetworkChange(netMobile: Int) {
        TODO("Not yet implemented")
    }

    private fun sendPortSipMessage(message: String?, broadIntent: Intent?) {
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

}