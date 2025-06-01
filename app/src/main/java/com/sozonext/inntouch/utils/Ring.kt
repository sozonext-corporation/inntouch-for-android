package com.sozonext.inntouch.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.sozonext.inntouch.R

class Ring(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Ring? = null

        fun getInstance(context: Context): Ring =
            instance ?: synchronized(this) {
                instance ?: Ring(context.applicationContext).also { instance = it }
            }
    }

    private var incomingRingtone: Ringtone? = null
    private var incomingRingtoneCount = 0

    private var outgoingToneMediaPlayer: MediaPlayer? = null

    private var savedMode = AudioManager.MODE_INVALID
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun startIncomingTone() {

        if (incomingRingtone?.isPlaying == true) {
            incomingRingtoneCount++
            return
        }

        if (incomingRingtone == null) {
            incomingRingtone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_RINGTONE_URI)
        }

        savedMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_RINGTONE

        incomingRingtone?.let {
            synchronized(it) {
                incomingRingtoneCount++
                it.play()
            }
        }
    }

    fun stopIncomingTone() {
        incomingRingtone?.let {
            synchronized(it) {
                if (--incomingRingtoneCount <= 0) {
                    it.stop()
                    incomingRingtone = null
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        }
    }

    fun startOutgoingTone() {
        stopOutgoingTone()
        try {
            val uri = Uri.Builder()
                .scheme("android.resource")
                .authority(context.packageName)
                .appendPath(R.raw.outgoing_tone.toString())
                .build()
            outgoingToneMediaPlayer = MediaPlayer.create(context, uri).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            outgoingToneMediaPlayer = null
        }
    }

    fun stopOutgoingTone() {
        outgoingToneMediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        outgoingToneMediaPlayer = null
    }

}