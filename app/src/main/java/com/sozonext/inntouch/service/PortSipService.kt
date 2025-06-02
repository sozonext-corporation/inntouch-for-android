package com.sozonext.inntouch.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.portsip.OnPortSIPEvent
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_UDP
import com.portsip.PortSipErrorcode
import com.sozonext.inntouch.R
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.receiver.NetWorkReceiver
import com.sozonext.inntouch.ui.activity.MainActivity
import com.sozonext.inntouch.utils.DataStoreUtil
import com.sozonext.inntouch.utils.Ring
import com.sozonext.inntouch.utils.Session
import com.sozonext.inntouch.utils.SessionManager
import com.sozonext.inntouch.utils.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Random

class PortSipService : Service(), OnPortSIPEvent, NetWorkReceiver.NetWorkListener {

    private val tag: String = PortSipService::class.java.simpleName
    private var pushToken: String = ""

    private val portSipSdk = MyApplication.portSipSdk

    companion object {

        private const val SERVICE_NOTIFICATION = 31414
        private const val APP_ID = "com.sozonext.inntouch"

        const val ACTION_SIP_REGISTER: String = "com.sozonext.inntouch.action.SIP_REGISTER"
        const val ACTION_SIP_UNREGISTER: String = "com.sozonext.inntouch.action.SIP_UNREGISTER"

        const val ACTION_ON_INVITE_ANSWERED: String = "com.sozonext.inntouch.action.ACTION_ON_INVITE_ANSWERED"
        const val ACTION_ON_INCOMING_CALL: String = "com.sozonext.inntouch.action.ACTION_ON_INCOMING_CALL"
        const val ACTION_ON_REJECT: String = "com.sozonext.inntouch.action.ACTION_ON_REJECT"

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

        const val EXTRA_PUSH_TOKEN: String = "com.sozonext.inntouch.action.EXTRA_PUSH_TOKEN"
        const val EXTRA_TARGET_EXTENSION_DISPLAY_NAME: String = "com.sozonext.inntouch.action.EXTRA_TARGET_EXTENSION_DISPLAY_NAME"
        const val EXTRA_REGISTER_STATE: String = "com.sozonext.inntouch.action.EXTRA_REGISTER_STATE"

        fun startServiceCompatibility(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }

    }

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mNetWorkReceiver: NetWorkReceiver

    override fun onCreate() {
        super.onCreate()

        val channelId = getString(R.string.channel_id)
        val highChannelId = "$channelId.HIGH"

        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
        val callChannel = NotificationChannel(highChannelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
        channel.enableLights(true)
        mNotificationManager.createNotificationChannel(channel)
        mNotificationManager.createNotificationChannel(callChannel)

        //
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this, channelId)

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Service Running")
            .setContentIntent(contentIntent)
            .build()
        startForeground(
            SERVICE_NOTIFICATION, builder.build(), (ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        )

        //
        val filter = IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        mNetWorkReceiver = NetWorkReceiver()
        mNetWorkReceiver.setListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mNetWorkReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mNetWorkReceiver, filter)
        }

        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("FCM", "FCM token fetch failed", task.exception)
                        return@addOnCompleteListener
                    }
                    pushToken = task.result
                    if (!TextUtils.isEmpty(pushToken) && SessionManager.getInstance().isRegistered) {
                        val pushMessage = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=${APP_ID}"
                        portSipSdk.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage)
                        portSipSdk.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage)
                        portSipSdk.refreshRegistration(0)
                    }
                }
        } catch (e: IllegalStateException) {
            Log.d("FCM", "Token fetch failed with exception: ${e.message}")
        }
    }

    /**
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        portSipSdk.destroyConference()

        if (mNetWorkReceiver != null) {
            unregisterReceiver(mNetWorkReceiver)
        }
//        if (mCpuLock != null) {
//            mCpuLock.release()
//        }
        val channelId = getString(R.string.channel_id)
        mNotificationManager.cancelAll()
        mNotificationManager.deleteNotificationChannel(channelId)
        mNotificationManager.deleteNotificationChannel("$channelId.HIGH")
        portSipSdk.removeUser()
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)

//        val channelId = getString(R.string.channel_id)
//        val channelName = getString(R.string.channel_name)
//
//        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
//        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        manager.createNotificationChannel(channel)
//
//        val notification = NotificationCompat.Builder(this, channelId)
//            .setContentTitle(channelName)
//            .setContentText("$channelName is Running")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .build()
//
//        startForeground(1, notification)

        if (intent != null) {
            when (intent.action) {
                ACTION_SIP_REGISTER -> {
                    if (SessionManager.getInstance().isOnline) {
                        unregister()
                    }
                    register()
                }
                // ACTION_PUSH_TOKEN
                ACTION_PUSH_TOKEN -> {
                }
                // ACTION_PUSH_MESSAGE
                ACTION_PUSH_MESSAGE -> {
                }
            }
        }
        return result
    }

    /**
     * onBind
     */
    override fun onBind(p0: Intent): IBinder? {
        return null
    }

    /**
     * onRegisterSuccess
     */
    override fun onRegisterSuccess(state: String, p1: Int, p2: String) {
        Log.d(tag, "onRegisterSuccess: $state, $p1, $p2")
        SessionManager.getInstance().isRegistered = true
        val broadIntent = Intent(REGISTER_CHANGE_ACTION)
        broadIntent.putExtra(EXTRA_REGISTER_STATE, state)
    }

    /**
     * onRegisterFailure
     */
    override fun onRegisterFailure(state: String, p1: Int, p2: String) {
        Log.d(tag, "onRegisterFailure: $state, $p1, $p2")
        SessionManager.getInstance().isRegistered = false
        SessionManager.getInstance().resetAll()
        val broadIntent = Intent(REGISTER_CHANGE_ACTION)
        broadIntent.putExtra(EXTRA_REGISTER_STATE, state)
    }

    /**
     * onInviteIncoming
     */
    override fun onInviteIncoming(sessionId: Long, callerDisplayName: String, caller: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        Log.d(tag, "onInviteIncoming: $sessionId, $callerDisplayName, $caller")

        if (SessionManager.getInstance().findIncomingCall() != null) {
            portSipSdk.rejectCall(sessionId, 486)
            return
        }

        Ring.getInstance(this).startIncomingTone()

        val session: Session = SessionManager.getInstance().findIdleSession() ?: return
        session.sessionStatus = SessionStatus.INCOMING
        session.sessionId = sessionId
        session.targetExtensionNumber = caller
        session.targetExtensionDisplayName = callerDisplayName

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "MyApp:WakeLockTag"
        )
        wakeLock.acquire(3000L)
        Thread.sleep(1000)
        val intent = Intent(ACTION_ON_INCOMING_CALL)
        intent.setPackage(packageName)
        intent.putExtra(EXTRA_TARGET_EXTENSION_DISPLAY_NAME, callerDisplayName)
        sendBroadcast(intent)

        // ToDo
    }

    override fun onInviteTrying(p0: Long) {}

    /**
     * onInviteSessionProgress
     */
    override fun onInviteSessionProgress(sessionId: Long, p1: String, p2: String, existsEarlyMedia: Boolean, p4: Boolean, p5: Boolean, p6: String) {
        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.existsEarlyMedia = existsEarlyMedia
    }

    /**
     * onInviteRinging
     */
    override fun onInviteRinging(sessionId: Long, p1: String, p2: Int, p3: String) {
        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        if (!session.existsEarlyMedia) {
            Ring.getInstance(this).startOutgoingTone();
        }
    }

    /**
     * onInviteAnswered
     */
    override fun onInviteAnswered(sessionId: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        Log.d(tag, "onInviteAnswered($sessionId)")

        Ring.getInstance(this).stopOutgoingTone();

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.CONNECTED

        // ToDo
    }

    /**
     * onInviteFailure
     */
    override fun onInviteFailure(sessionId: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {

        Ring.getInstance(this).stopOutgoingTone()

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.FAILED
        session.sessionId = sessionId

        val intent = Intent(ACTION_ON_REJECT)
        intent.setPackage(packageName)
        sendBroadcast(intent)
        // ToDo
    }

    /**
     * onInviteUpdated
     */
    override fun onInviteUpdated(sessionId: Long, p1: String, p2: String, p3: String, p4: Boolean, p5: Boolean, p6: Boolean, p7: String) {

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.CONNECTED

        // ToDo
    }

    /**
     * onInviteConnected
     */
    override fun onInviteConnected(sessionId: Long) {
        Log.d(tag, "onInviteConnected($sessionId)")

        val intent = Intent(ACTION_ON_INVITE_ANSWERED)
        intent.setPackage(packageName)
        sendBroadcast(intent)

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.CONNECTED
        session.sessionId = sessionId

        // ToDo
    }

    override fun onInviteBeginingForward(p0: String) {}

    /**
     * onInviteClosed
     */
    override fun onInviteClosed(sessionId: Long, p1: String) {
        Log.d(tag, "onInviteClosed: $sessionId, $p1")

        Ring.getInstance(this).stopIncomingTone()

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.CLOSED
        session.sessionId = sessionId

        val intent = Intent(ACTION_ON_REJECT)
        intent.setPackage(packageName)
        sendBroadcast(intent)

        // ToDo
    }

    override fun onDialogStateUpdated(p0: String, p1: String, p2: String, p3: String) {}

    override fun onRemoteHold(p0: Long) {}

    override fun onRemoteUnHold(p0: Long, p1: String, p2: String, p3: Boolean, p4: Boolean) {}

    override fun onReceivedRefer(p0: Long, p1: Long, p2: String, p3: String, p4: String) {}

    /**
     * onReferAccepted
     */
    override fun onReferAccepted(p0: Long) {
        Log.d(tag, "onReferAccepted: $p0")
        // ToDo
    }

    override fun onReferRejected(p0: Long, p1: String, p2: Int) {}

    override fun onTransferTrying(p0: Long) {}

    override fun onTransferRinging(p0: Long) {}

    /**
     * onACTVTransferSuccess
     */
    override fun onACTVTransferSuccess(p0: Long) {
        Log.d(tag, "onACTVTransferSuccess: $p0")
        // ToDo
    }

    /**
     * onACTVTransferFailure
     */
    override fun onACTVTransferFailure(p0: Long, p1: String, p2: Int) {
        Log.d(tag, "onACTVTransferFailure: $p0, $p1, $p2")
        // ToDo
    }

    override fun onReceivedSignaling(p0: Long, p1: String) {}

    override fun onSendingSignaling(p0: Long, p1: String) {}

    override fun onWaitingVoiceMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {}

    override fun onWaitingFaxMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {}

    override fun onRecvDtmfTone(p0: Long, p1: Int) {}

    override fun onRecvOptions(p0: String) {}

    override fun onRecvInfo(p0: String) {}

    override fun onRecvNotifyOfSubscription(p0: Long, p1: String, p2: ByteArray, p3: Int) {}

    /**
     * onPresenceRecvSubscribe
     */
    override fun onPresenceRecvSubscribe(p0: Long, p1: String, p2: String, p3: String) {
        Log.d(tag, "onPresenceRecvSubscribe: $p0, $p1, $p2, $p3")
    }

    /**
     * onPresenceOnline
     */
    override fun onPresenceOnline(p0: String, p1: String, p2: String) {
        Log.d(tag, "onPresenceOnline: $p0, $p1, $p2")
    }

    /**
     * onPresenceOffline
     */
    override fun onPresenceOffline(p0: String, p1: String) {
        Log.d(tag, "onPresenceOffline: $p0, $p1")
    }

    override fun onRecvMessage(p0: Long, p1: String, p2: String, p3: ByteArray, p4: Int) {}

    override fun onRecvOutOfDialogMessage(p0: String, p1: String, p2: String, p3: String, p4: String, p5: String, p6: ByteArray, p7: Int, p8: String) {}

    override fun onSendMessageSuccess(p0: Long, p1: Long, p2: String) {}

    override fun onSendMessageFailure(p0: Long, p1: Long, p2: String, p3: Int, p4: String) {}

    override fun onSendOutOfDialogMessageSuccess(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String) {}

    override fun onSendOutOfDialogMessageFailure(p0: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {}

    override fun onSubscriptionFailure(p0: Long, p1: Int) {}

    override fun onSubscriptionTerminated(p0: Long) {}

    override fun onPlayFileFinished(p0: Long, p1: String) {}

    override fun onStatistics(p0: Long, p1: String) {}

    /**
     * onAudioDeviceChanged
     */
    override fun onAudioDeviceChanged(p0: PortSipEnumDefine.AudioDevice, p1: MutableSet<PortSipEnumDefine.AudioDevice>) {
        Log.d(tag, "onAudioDeviceChanged: $p0, $p1")
        // ToDo
    }

    override fun onAudioFocusChange(p0: Int) {}

    override fun onRTPPacketCallback(p0: Long, p1: Int, p2: Int, p3: ByteArray, p4: Int) {}

    override fun onAudioRawCallback(p0: Long, p1: Int, p2: ByteArray, p3: Int, p4: Int) {}

    override fun onVideoRawCallback(p0: Long, p1: Int, p2: Int, p3: Int, p4: ByteArray, p5: Int) {}

    /**
     * onNetworkChange
     */
    override fun onNetworkChange(netMobile: Int) {
        // ToDo
    }

    /**
     * register
     */
    private fun register() {
        portSipSdk.setOnPortSIPEvent(this)

        SessionManager.getInstance().isOnline = true

        val externalFilesPath = getExternalFilesDir(null)!!.absolutePath
        val tlsCertificatesRootPath = "$externalFilesPath/certs"
        val localSIPPort = 5060 + Random().nextInt(60000)

        var result = portSipSdk.initialize(
            ENUM_TRANSPORT_UDP, "0.0.0.0", localSIPPort, PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, externalFilesPath, 8, "PortSIP SDK for Android", 0, 0, tlsCertificatesRootPath, "", false, null
        )

        if (result != PortSipErrorcode.ECoreErrorNone) {
            SessionManager.getInstance().resetAll()
        } else {
            result = portSipSdk.setLicenseKey("HO1KH-5ZQ8W-BJSZC-NIDRI-TH5UD")
            if (result == PortSipErrorcode.ECoreWrongLicenseKey) {
                Log.d(tag, "The wrong license key was detected, please check with sales@portsip.com or support@portsip.com")
            } else if (result == PortSipErrorcode.ECoreTrialVersionLicenseKey) {
                Log.d(tag, "This Is Trial Version")
            }
        }

        val sipServer: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.SIP_SERVER).first().toString()
        }
        val sipDomain: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.SIP_DOMAIN).first().toString()
        }
        val exceptionNumber: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.EXTENSION_NUMBER).first().toString()
        }
        val exceptionPassword: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.EXTENSION_PASSWORD).first().toString()
        }

        portSipSdk.removeUser()
        result = portSipSdk.setUser(
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
            Log.d(tag, "setUser failure ErrorCode = $result")
            SessionManager.getInstance().resetAll()
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
            Log.d(tag, "registerServer failure ErrorCode =$result")
            portSipSdk.unRegisterServer(100)
            SessionManager.getInstance().resetAll()
        }
    }

    /**
     * unregister
     */
    private fun unregister() {
        portSipSdk.unRegisterServer(100)
        portSipSdk.removeUser()
        portSipSdk.unInitialize()
        SessionManager.getInstance().isOnline = false
        SessionManager.getInstance().isRegistered = false
    }

}