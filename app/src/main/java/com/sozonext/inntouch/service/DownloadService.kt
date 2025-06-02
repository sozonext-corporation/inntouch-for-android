package com.sozonext.inntouch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.sozonext.inntouch.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException

class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startDownloadNotification()
    }

    private fun startDownloadNotification() {
        val notification = NotificationCompat.Builder(this, "download_channel")
            .setContentTitle("ダウンロード中")
            .setContentText("アプリのアップデートを取得しています")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 必ず ID を指定して startForeground を呼び出す
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "download_channel",
            "アップデート通知",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ダウンロード処理を開始
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
