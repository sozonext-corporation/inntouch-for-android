package com.sozonext.inntouch.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipEnumDefine
import com.sozonext.inntouch.R
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.utils.Ring
import com.sozonext.inntouch.utils.Session
import com.sozonext.inntouch.utils.SessionManager
import com.sozonext.inntouch.utils.SessionStatus

class VideoCallActivity : AppCompatActivity(), View.OnClickListener {

    private val tag = this::class.java.simpleName

    private val portSipSdk = MyApplication.portSipSdk

    private var localVideoView: PortSIPVideoRenderer? = null
    private var remoteVideoView: PortSIPVideoRenderer? = null

    private var isFrontCamera = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_call)

        // On Click Listener
        this.findViewById<ImageButton>(R.id.buttonCamera).setOnClickListener(this)
        this.findViewById<ImageButton>(R.id.buttonEnd).setOnClickListener(this)
        this.findViewById<ImageButton>(R.id.buttonSwitch).setOnClickListener(this)

        // Start Tone
        Ring.getInstance(this).startOutgoingTone()

        // TODO
        portSipSdk.clearAudioCodec()
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA);
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729)
        portSipSdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_OPUS)

        portSipSdk.clearVideoCodec()
        portSipSdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_H264)
        // portSipSdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEO_CODEC_VP8)
        // portSipSdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEO_CODEC_VP9)
        portSipSdk.setVideoResolution(1920, 1200)

        localVideoView = findViewById<PortSIPVideoRenderer>(R.id.local_video_view)
        remoteVideoView = findViewById<PortSIPVideoRenderer>(R.id.remote_video_view)

        portSipSdk.setVideoDeviceId(1)
        portSipSdk.setVideoResolution(640, 480)
        portSipSdk.displayLocalVideo(true, true, localVideoView)

        val sessionId = portSipSdk.call("901", true, true)
        if (sessionId <= 0) {
            Toast.makeText(this, "Call failure: $sessionId", Toast.LENGTH_SHORT).show()
            return
        }
        portSipSdk.sendVideo(sessionId, true)

        val session: Session = SessionManager.getInstance().getCurrentSession() ?: return

        val extensionDisplayName = ""

        session.sessionStatus = SessionStatus.TRYING
        session.sessionId = sessionId
        session.targetExtensionNumber = "901"
        session.targetExtensionDisplayName = extensionDisplayName
        portSipSdk.setRemoteVideoWindow(session.sessionId, remoteVideoView)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (localVideoView != null) {
            portSipSdk.displayLocalVideo(false, false, null)
            localVideoView!!.release()
        }
    }

    /**
     * onClick
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            // ビデオ (ON/OFF)
            R.id.buttonCamera -> camera()
            // 終了
            R.id.buttonEnd -> end()
            // 切り替え
            R.id.buttonSwitch -> switch()
        }
    }

    /**
     * ビデオ (ON/OFF)
     */
    private fun camera() {

    }

    /**
     * 終了
     */
    private fun end() {
        finish()
    }

    /**
     * 切り替え
     */
    private fun switch() {

    }

}