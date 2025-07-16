package com.sozonext.inntouch.utils

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class AudioManagerUtil(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var contentObserver: ContentObserver? = null

    @SuppressLint("ServiceCast")
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val defaultRingVolume: Int = (audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) * 0.5).toInt()
    private val defaultMusicVolume: Int = (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.5).toInt()
    private val defaultVoiceCallVolume: Int = (audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) * 0.5).toInt()

    fun startLockVolume() {

        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        audioManager.setStreamVolume(AudioManager.STREAM_RING, defaultRingVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, defaultMusicVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, defaultVoiceCallVolume, 0)

        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
                if (audioManager.getStreamVolume(AudioManager.STREAM_RING) < defaultRingVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, defaultRingVolume, 0)
                }
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < defaultMusicVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, defaultMusicVolume, 0)
                }
                if (audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) < defaultVoiceCallVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, defaultVoiceCallVolume, 0)
                }
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, contentObserver!!)
    }

    fun stopLockVolume() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            contentObserver = null
        }
    }
}