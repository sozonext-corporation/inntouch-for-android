package com.sozonext.inntouch.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.net.http.SslError
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import com.sozonext.inntouch.BuildConfig
import com.sozonext.inntouch.R
import com.sozonext.inntouch.service.PortSipService
import com.sozonext.inntouch.service.PortSipService.Companion.EXTRA_TARGET_EXTENSION_DISPLAY_NAME
import com.sozonext.inntouch.ui.JavaScriptInterface
import com.sozonext.inntouch.utils.DataStoreUtil
import com.sozonext.inntouch.utils.KioskUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val tag: String = MainActivity::class.java.simpleName

    private lateinit var webView: WebView

    private var counter = 0
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_CODE_CAMERA_AND_AUDIO = 2
    }

    @SuppressLint("SetJavaScriptEnabled", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // On Click Listener
        this.findViewById<Button>(R.id.button).setOnClickListener(this)

        // Permissions
        requestPermissions()

        // Kiosk Mode
        KioskUtil(this).start(this)

        // WebView
        webView = findViewById(R.id.webView)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavaScriptInterface(this), "Android")

        // Debug Mode
        webView.settings.allowContentAccess = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                if (!BuildConfig.DEBUG) {
                    super.onReceivedSslError(view, handler, error) // Release
                } else {
                    handler?.proceed() // Debug
                }
            }
//            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
//                super.shouldOverrideUrlLoading(view, request)
//                view.loadUrl(request.url.toString())
//                return true
//            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            // 21 onHideCustomView
            override fun onHideCustomView() {
                super.onHideCustomView()
                webView.removeAllViews()
            }

            // 47 onShowCustomView
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                webView.addView(
                    view, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )
            }

            // 33 onPermissionRequest
            override fun onPermissionRequest(request: PermissionRequest) {
                super.onPermissionRequest(request)
                request.grant(request.resources)
            }

        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "通知が許可されました", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "通知は許可されていません", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "BatteryLife", "ServiceCast")
    override fun onResume() {
        super.onResume()

        // BroadcastReceiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(PortSipService.ACTION_ON_INVITE_ANSWERED)
            addAction(PortSipService.ACTION_ON_INCOMING_CALL)
            addAction(PortSipService.ACTION_ON_REJECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }

        // WebViewの初期表示
        val startUrl: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.START_URL).first().toString()
        }
        if (startUrl.isNotEmpty()) {
            webView.loadUrl(startUrl)
            webView.requestFocus();
        } else {
            launchQRCodeActivity()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    @SuppressLint("BatteryLife")
    private fun requestPermissions() {

        // Camera & Audio
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ||
            PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), REQUEST_CODE_CAMERA_AND_AUDIO
            )
        }

        // Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "通知の権限が必要です。設定から許可してください。",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("設定を開く") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    }.show()
            }
        }

        // Backend Service (ToDo)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData("package:$packageName".toUri())
            this.startActivity(intent)
        }
    }

    @SuppressLint("ImplicitSamInstance")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        when (requestCode) {
            REQUEST_CODE_CAMERA_AND_AUDIO -> {
                var i = 0
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "you must grant the permission " + permissions[i], Toast.LENGTH_SHORT).show()
                        stopService(Intent(this, PortSipService::class.java))
                        exitProcess(0)
                    }
                }
            }
        }
    }


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                PortSipService.ACTION_ON_INVITE_ANSWERED -> {
                    Log.d(tag, "onInviteAnswered()")
                    webView.evaluateJavascript("javascript:onInviteAnswered()", null)
                }

                PortSipService.ACTION_ON_INCOMING_CALL -> {
                    Log.d(tag, "onIncomingCall()")
                    val callerDisplayName = intent.getStringExtra(EXTRA_TARGET_EXTENSION_DISPLAY_NAME)
                    webView.evaluateJavascript("javascript:onIncomingCall('$callerDisplayName')", null)
                }

                PortSipService.ACTION_ON_REJECT -> {
                    Log.d(tag, "onReject()")
                    webView.evaluateJavascript("javascript:onReject()", null)
                }

                Intent.ACTION_BATTERY_CHANGED -> {
                    Log.d(tag, "onBatteryChargingStatusChanged()")
                    intent.let {
                        val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                        webView.evaluateJavascript("javascript:onBatteryChargingStatusChanged(${isCharging})", null)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(tag, "onUsbAttachingStatusChanged(true)")
                    webView.evaluateJavascript("javascript:onUsbAttachingStatusChanged(true)", null)
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(tag, "onUsbAttachingStatusChanged(false)")
                    webView.evaluateJavascript("javascript:onUsbAttachingStatusChanged(false)", null)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        if (counter == 0) {
            handler.postDelayed({ counter = 0 }, 5 * 1000)
        }
        if (++counter >= 5) {
            counter = 0
            openPasswordDialog()
        }
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val event: String = result.data?.getStringExtra("event") ?: ""
            when (event) {
                "clearCache" -> clearCache()
                "navigateStartUrl" -> navigateStartUrl()
                "navigateConfigUrl" -> navigateConfigUrl()
                "launchQRCodeActivity" -> launchQRCodeActivity()
            }
        }
    }

    private fun openPasswordDialog() {
        val dialogEditPassword = layoutInflater.inflate(R.layout.dialog_edit_password, null)
        val alertDialog = AlertDialog.Builder(this).setTitle("アプリ設定").setMessage("パスワードを入力してください。").setView(dialogEditPassword).setCancelable(false).setPositiveButton("OK") { _, _ -> }.setNegativeButton("キャンセル") { _, _ -> }.create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val inputPassword = dialogEditPassword.findViewById<EditText>(R.id.editTextPassword).text.toString()
                val password: String = runBlocking {
                    DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.PASSWORD).first().toString()
                }
                if (inputPassword == password) {
                    val intent = Intent(this, MenuActivity::class.java)
                    launcher.launch(intent)
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "パスワードが間違っています", Toast.LENGTH_SHORT).show()
                }
            }
        }
        alertDialog.show()
    }

    private fun clearCache() {
        webView.clearCache(true)
        webView.clearHistory()
        cacheDir.deleteRecursively()
        navigateStartUrl()
    }

    private fun navigateStartUrl() {
        val startUrl: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.START_URL).first().toString()
        }
        webView.loadUrl(startUrl)
        webView.requestFocus();
    }

    private fun navigateConfigUrl() {
        val startUrl: String = runBlocking {
            DataStoreUtil(applicationContext).getDataStoreValue(DataStoreUtil.CONFIG_URL).first().toString()
        }
        webView.loadUrl(startUrl)
        webView.requestFocus();
    }

    private fun launchQRCodeActivity() {
        val intent = Intent(this, QRCodeScannerActivity::class.java)
        launcher.launch(intent)
    }

}
