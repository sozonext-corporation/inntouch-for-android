package com.sozonext.inntouch.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class PermissionUtils(private val activity: Activity) {

    companion object {
        private const val REQUEST_CODE_CAMERA_AND_AUDIO = 2
        private const val REQUEST_CODE_READ_PHONE_STATE = 1001
    }

    /**
     * まとめて必要な権限をリクエストする
     */
    fun requestAllPermissions() {
        requestInstallUnknownAppsPermission()
        requestCameraAndAudioPermission()
        requestReadPhoneStatePermission()
        requestIgnoreBatteryOptimizations()
    }

    /**
     * 不明アプリインストール許可
     */
    private fun requestInstallUnknownAppsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${activity.packageName}".toUri()
                }
                activity.startActivity(intent)
            }
        }
    }

    /**
     * カメラ・マイク
     */
    private fun requestCameraAndAudioPermission() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val notGranted = permissions.any {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_CAMERA_AND_AUDIO)
        }
    }

    /**
     * 電話状態
     */
    private fun requestReadPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_CODE_READ_PHONE_STATE
            )
        }
    }

    /**
     * バッテリー最適化除外
     */
    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(activity.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${activity.packageName}".toUri()
            }
            activity.startActivity(intent)
        }
    }
}