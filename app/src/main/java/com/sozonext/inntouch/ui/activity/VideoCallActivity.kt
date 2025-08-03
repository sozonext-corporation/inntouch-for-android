package com.sozonext.inntouch.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.portsip.PortSIPVideoRenderer
import com.sozonext.inntouch.R
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.utils.Session
import com.sozonext.inntouch.utils.SessionManager

class VideoCallActivity : AppCompatActivity(), View.OnClickListener {

    private val portSipSdk = MyApplication.portSipSdk

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_call)

        // On Click Listener
        this.findViewById<ImageButton>(R.id.buttonCamera).setOnClickListener(this)
        this.findViewById<ImageButton>(R.id.buttonEnd).setOnClickListener(this)
        this.findViewById<ImageButton>(R.id.buttonSwitch).setOnClickListener(this)

        val localVideoView = findViewById<PortSIPVideoRenderer>(R.id.local_video_view)
        val remoteVideoView = findViewById<PortSIPVideoRenderer>(R.id.remote_video_view)

        val session: Session = SessionManager.getInstance().getCurrentSession() ?: return
        portSipSdk.setVideoDeviceId(1)
        portSipSdk.setVideoResolution(640, 480)
        portSipSdk.displayLocalVideo(true, true, localVideoView)
        portSipSdk.setRemoteVideoWindow(session.sessionId, remoteVideoView)
        portSipSdk.updateCall(session.sessionId, true, true)
    }

    /**
     * onClick
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            // ビデオ (ON/OFF)
            R.id.buttonCamera -> {

            }
            // 終了
            R.id.buttonEnd -> {
                finish()
            }
            //切り替え
            R.id.buttonSwitch -> {

            }
        }
    }

}