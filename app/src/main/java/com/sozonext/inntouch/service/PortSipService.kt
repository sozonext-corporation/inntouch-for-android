package com.sozonext.inntouch.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.portsip.OnPortSIPEvent
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_UDP
import com.portsip.PortSipErrorcode
import com.sozonext.inntouch.R
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.receiver.NetWorkReceiver
import com.sozonext.inntouch.ui.activity.MainActivity
import com.sozonext.inntouch.utils.DataStoreUtils
import com.sozonext.inntouch.utils.Ring
import com.sozonext.inntouch.utils.Session
import com.sozonext.inntouch.utils.SessionManager
import com.sozonext.inntouch.utils.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Random


class PortSipService : Service(), OnPortSIPEvent, NetWorkReceiver.NetWorkListener {

    private val tag = this::class.java.simpleName

    private val portsip = MyApplication.portSipSdk

    private var pushToken: String = ""
    
    companion object {
        private const val CALL_NOTIFICATION = 31413
        private const val SERVICE_NOTIFICATION = 31414

        const val ACTION_SIP_REGISTER: String = "com.sozonext.inntouch.action.SIP_REGISTER"
        const val ACTION_SIP_UNREGISTER: String = "com.sozonext.inntouch.action.SIP_UNREGISTER"
        const val ACTION_PUSH_TOKEN: String = "com.sozonext.inntouch.action.PUSH_TOKEN"
        const val ACTION_PUSH_MESSAGE: String = "com.sozonext.inntouch.action.PUSH_MESSAGE"

        const val ACTION_ON_REJECT: String = "com.sozonext.inntouch.action.ACTION_ON_REJECT"

        const val ACTION_ON_INVITE_ANSWERED: String = "com.sozonext.inntouch.action.ACTION_ON_INVITE_ANSWERED"
        const val ACTION_ON_INCOMING_CALL: String = "com.sozonext.inntouch.action.ACTION_ON_INCOMING_CALL"


        const val ACTION_SIP_AUDIO_DEVICE_UPDATE: String = "com.sozonext.inntouch.action.ACTION_SIP_AUDIO_DEVICE_UPDATE"


        const val REGISTER_CHANGE_ACTION: String = "portsip.AndroidSample.Test.RegisterStatusChange"
        const val CALL_CHANGE_ACTION: String = "portsip.AndroidSample.Test.CallStatusChange"
        const val PRESENCE_CHANGE_ACTION: String = "portsip.AndroidSample.Test.PRESENCEStatusChange"

        const val EXTRA_PUSH_TOKEN: String = "com.sozonext.inntouch.action.EXTRA_PUSH_TOKEN"
        const val EXTRA_TARGET_EXTENSION_DISPLAY_NAME: String = "com.sozonext.inntouch.action.EXTRA_TARGET_EXTENSION_DISPLAY_NAME"
        const val EXTRA_REGISTER_STATE: String = "com.sozonext.inntouch.action.EXTRA_REGISTER_STATE"

        fun startServiceCompatible(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

    }

    private val callChannelId = "com.sozonext.inntouch.notification.call";
    private val serviceChannelId = "com.sozonext.inntouch.notification.service";

    private lateinit var notificationManager: NotificationManager
    private lateinit var mNetWorkReceiver: NetWorkReceiver

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        initializeNotification()

        showServiceNotification()

        // registerReceiver
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
                        return@addOnCompleteListener
                    }
                    pushToken = task.result
                    if (!TextUtils.isEmpty(pushToken) && SessionManager.getInstance().isRegistered) {
                        val pushMessage = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$packageName"
                        portsip.addSipMessageHeader(-1, "REGISTER", 1, "X-Push", pushMessage)
                        portsip.refreshRegistration(0)
                    }
                }
        } catch (e: java.lang.IllegalStateException) {
            Log.d("", e.toString())
        }
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        portsip.destroyConference()

        Ring.getInstance(this).stopIncomingTone()
        Ring.getInstance(this).stopOutgoingTone()

        unregisterReceiver(mNetWorkReceiver)
//        if (mCpuLock != null) {
//            mCpuLock.release()
//        }
        notificationManager.cancelAll()
        notificationManager.deleteNotificationChannel(callChannelId)
        notificationManager.deleteNotificationChannel(serviceChannelId)
        portsip.removeUser()
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ToDo
        val result = super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            when (intent.action) {
                ACTION_SIP_REGISTER -> {
                    if (SessionManager.getInstance().isOnline) {
                        unregister()
                    }
                    initialize()
                    register()
                }

                ACTION_SIP_UNREGISTER -> {
                    unregister()
                }
                // ACTION_PUSH_TOKEN
                ACTION_PUSH_TOKEN -> {
                    pushToken = intent.getStringExtra(EXTRA_PUSH_TOKEN)!!
                    if (!TextUtils.isEmpty(pushToken) && SessionManager.getInstance().isRegistered) {
                        val pushMessage = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$packageName"
                        portsip.addSipMessageHeader(-1, "REGISTER", 1, "X-Push", pushMessage)
                        portsip.refreshRegistration(0)
                    }
                }
                // ACTION_PUSH_MESSAGE
                ACTION_PUSH_MESSAGE -> {
                    if (!SessionManager.getInstance().isOnline) {
                        initialize()
                    }
                    if (!SessionManager.getInstance().isRegistered) {
                        register()
                    }
                }
            }
        }
        return result
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
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onInviteIncoming(sessionId: Long, callerDisplayName: String, caller: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        Log.d(tag, "onInviteIncoming: $sessionId, $callerDisplayName, $caller")

        if (SessionManager.getInstance().findIncomingCall() != null) {
            portsip.rejectCall(sessionId, 486)
            return
        }

        Ring.getInstance(this).startIncomingTone()

        val session: Session = SessionManager.getInstance().findIdleSession() ?: return
        session.sessionStatus = SessionStatus.INCOMING
        session.sessionId = sessionId
        session.targetExtensionNumber = caller
        session.targetExtensionDisplayName = callerDisplayName

        val intent = Intent(ACTION_ON_INCOMING_CALL)
        intent.setPackage(packageName)
        intent.putExtra(EXTRA_TARGET_EXTENSION_DISPLAY_NAME, callerDisplayName)
        sendBroadcast(intent)
    }

    /**
     * onInviteTrying()
     */
    override fun onInviteTrying(p0: Long) {
        Log.d(tag, "onInviteTrying()")
    }

    /**
     * onInviteSessionProgress
     */
    override fun onInviteSessionProgress(sessionId: Long, p1: String, p2: String, existsEarlyMedia: Boolean, p4: Boolean, p5: Boolean, p6: String) {
        Log.d(tag, "onInviteSessionProgress()")
        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.existsEarlyMedia = existsEarlyMedia
    }

    /**
     * onInviteRinging
     */
    override fun onInviteRinging(sessionId: Long, p1: String, p2: Int, p3: String) {
        Log.d(tag, "onInviteRinging()")
        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        if (!session.existsEarlyMedia) {
            Ring.getInstance(this).startOutgoingTone();
        }
    }

    /**
     * onInviteAnswered
     */
    override fun onInviteAnswered(sessionId: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String, p7: Boolean, p8: Boolean, p9: String) {
        Log.d(tag, "onInviteAnswered()")

        Ring.getInstance(this).stopOutgoingTone();

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.CONNECTED

        // ToDo
    }

    /**
     * onInviteFailure
     */
    override fun onInviteFailure(sessionId: Long, p1: String, p2: String, p3: String, p4: String, p5: String, p6: Int, p7: String) {
        Log.d(tag, "onInviteFailure()")

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
        Log.d(tag, "onInviteUpdated()")

        val session: Session = SessionManager.getInstance().findSessionBySessionId(sessionId) ?: return
        session.sessionStatus = SessionStatus.CONNECTED

        // ToDo
    }

    /**
     * onInviteConnected
     */
    override fun onInviteConnected(sessionId: Long) {
        Log.d(tag, "onInviteConnected()")

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
        Log.d(tag, "onInviteClosed()")

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

    override fun onRemoteHold(p0: Long) {
        Log.d(tag, "onRemoteHold()")
    }

    override fun onRemoteUnHold(p0: Long, p1: String, p2: String, p3: Boolean, p4: Boolean) {
        Log.d(tag, "onRemoteUnHold()")
    }

    override fun onReceivedRefer(p0: Long, p1: Long, p2: String, p3: String, p4: String) {}

    override fun onReferAccepted(p0: Long) {}

    override fun onReferRejected(p0: Long, p1: String, p2: Int) {}

    override fun onTransferTrying(p0: Long) {}

    override fun onTransferRinging(p0: Long) {
        Log.d(tag, "onTransferRinging()")
    }

    /**
     * onACTVTransferSuccess
     */
    override fun onACTVTransferSuccess(p0: Long) {
        Log.d(tag, "onACTVTransferSuccess()")
        // ToDo
    }

    /**
     * onACTVTransferFailure
     */
    override fun onACTVTransferFailure(p0: Long, p1: String, p2: Int) {
        Log.d(tag, "onACTVTransferFailure()")
        // ToDo
    }

    override fun onReceivedSignaling(p0: Long, p1: String) {}

    override fun onSendingSignaling(p0: Long, p1: String) {}

    override fun onWaitingVoiceMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {
        Log.d(tag, "onWaitingVoiceMessage()")
    }

    override fun onWaitingFaxMessage(p0: String, p1: Int, p2: Int, p3: Int, p4: Int) {
        Log.d(tag, "onWaitingFaxMessage()")
    }

    override fun onRecvDtmfTone(p0: Long, p1: Int) {}

    override fun onRecvOptions(p0: String) {}

    override fun onRecvInfo(p0: String) {}

    override fun onRecvNotifyOfSubscription(p0: Long, p1: String, p2: ByteArray, p3: Int) {
        Log.d(tag, "onRecvNotifyOfSubscription()")
    }

    override fun onPresenceRecvSubscribe(p0: Long, p1: String, p2: String, p3: String) {}

    override fun onPresenceOnline(p0: String, p1: String, p2: String) {}

    override fun onPresenceOffline(p0: String, p1: String) {}

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
        Log.d(tag, "onAudioDeviceChanged()")
        // ToDo
    }

    override fun onAudioFocusChange(p0: Int) {}

    override fun onRTPPacketCallback(p0: Long, p1: Int, p2: Int, p3: ByteArray, p4: Int) {}

    override fun onAudioRawCallback(p0: Long, p1: Int, p2: ByteArray, p3: Int, p4: Int) {}

    override fun onVideoRawCallback(p0: Long, p1: Int, p2: Int, p3: Int, p4: ByteArray, p5: Int) {}

    override fun onNetworkChange(netMobile: Int) {
        Log.d(tag, "onNetworkChange()")
        // ToDo
    }

    private fun initializeNotification() {

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            // 音声通話/ビデオ通話の通知
            val callChannel = NotificationChannel(callChannelId, "音声通話/ビデオ通話の通知", NotificationManager.IMPORTANCE_HIGH)
            callChannel.vibrationPattern = longArrayOf(0, 500, 200, 500)
            callChannel.setSound(null, null)
            callChannel.enableVibration(true)
            callChannel.setAllowBubbles(true)
            callChannel.setShowBadge(false)
            callChannel.enableLights(true)
            callChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(callChannel)

            // サービスの通知
            val serviceChannel = NotificationChannel(serviceChannelId, "サービスの通知", NotificationManager.IMPORTANCE_LOW)
            serviceChannel.enableVibration(false)
            serviceChannel.enableLights(false)
            serviceChannel.setShowBadge(false)
            serviceChannel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            notificationManager.createNotificationChannel(serviceChannel)
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, callChannelId)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Service Running")
            .setContentIntent(contentIntent)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            ) {
                startForeground(
                    SERVICE_NOTIFICATION, builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(
                    SERVICE_NOTIFICATION, builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }
        } else {
            startForeground(SERVICE_NOTIFICATION, builder.build())
        }
    }

    /**
     * initialize SDK
     */
    private fun initialize() {
        portsip.setOnPortSIPEvent(this)
        SessionManager.getInstance().isOnline = true
        val externalFilesPath = getExternalFilesDir(null)!!.absolutePath
        val tlsCertificatesRootPath = "$externalFilesPath/certs"
        val localSIPPort = 5060 + Random().nextInt(60000)
        var result = portsip.initialize(
            ENUM_TRANSPORT_UDP, "0.0.0.0", localSIPPort, PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, externalFilesPath, 8, "portsip SDK for Android", 0, 0, tlsCertificatesRootPath, "", false, null
        )
        if (result != PortSipErrorcode.ECoreErrorNone) {
            SessionManager.getInstance().resetAll()
        } else {
            result = portsip.setLicenseKey("HO1KH-5ZQ8W-BJSZC-NIDRI-TH5UD")
            if (result == PortSipErrorcode.ECoreWrongLicenseKey) {
                Log.d(tag, "The wrong license key was detected, please check with sales@portsip.com or support@portsip.com")
            } else if (result == PortSipErrorcode.ECoreTrialVersionLicenseKey) {
                Log.d(tag, "This Is Trial Version")
            }
        }
    }

    /**
     * register
     */
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

        portsip.removeUser()
        var result = portsip.setUser(
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

        portsip.enableAudioManager(true)

        portsip.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)
        portsip.setVideoDeviceId(1)

        portsip.setSrtpPolicy(0)
        portsip.clearAudioCodec()
        portsip.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA);
        portsip.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
        portsip.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729)
        portsip.clearVideoCodec()
        portsip.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_H264)
        portsip.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP8)
        portsip.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP9)
        portsip.setVideoResolution(1920, 1200)

        portsip.enable3GppTags(false)

        if (!TextUtils.isEmpty(pushToken)) {
            Log.d("XXX", pushToken)
            val message = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$packageName"
            portsip.addSipMessageHeader(-1, "REGISTER", 1, "X-Push", message);
        }

        result = portsip.registerServer(60, 3)
        if (result != PortSipErrorcode.ECoreErrorNone) {
            Log.d(tag, "registerServer failure ErrorCode =$result")
            portsip.unRegisterServer(100)
            SessionManager.getInstance().resetAll()
        }
    }

    /**
     * unregister
     */
    private fun unregister() {
        portsip.unRegisterServer(100)
        portsip.removeUser()
        portsip.unInitialize()
        SessionManager.getInstance().isOnline = false
        SessionManager.getInstance().isRegistered = false
    }

    private fun refreshPushToken(token: String) {
        val pushMessage = "device-os=android;device-uid=$token;allow-call-push=true;allow-message-push=true;app-id=$packageName"
        portsip.addSipMessageHeader(-1, "REGISTER", 1, "X-Push", pushMessage)
        portsip.refreshRegistration(0)
    }


    private fun showServiceNotification() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(this, 0,  /*requestCode*/intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, serviceChannelId)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Service Running")
            .setContentIntent(contentIntent)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                SERVICE_NOTIFICATION, builder.build(), (
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                                or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                                or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            )
        } else {
            startForeground(SERVICE_NOTIFICATION, builder.build())
        }
    }

    private fun showCallNotification(context: Context, intent: Intent, contentTitle: String, contentText: String) {
        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, callChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
        notificationManager.notify(PortSipService.CALL_NOTIFICATION, builder.build())
    }

    fun sendPortSipMessage(message: String?, broadIntent: Intent?) {
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, serviceChannelId)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sip Notify")
            .setContentText(message)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(1, builder.build())
        sendBroadcast(broadIntent)
    }

}
// allSessionClosed()
// onSessionChange()