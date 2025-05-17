package com.sozonext.inntouch.ui.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sozonext.inntouch.R
import com.sozonext.inntouch.service.PortSipService
import com.sozonext.inntouch.service.PortSipService.Companion.ACTION_INVITE_ANSWERED
import com.sozonext.inntouch.ui.JavaScriptInterface
import com.sozonext.inntouch.utils.DataStoreUtils
import com.sozonext.inntouch.utils.KioskUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.log

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val tag: String = MainActivity::class.java.simpleName

    private lateinit var webView: WebView

    private var counter = 0
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // On Click Listener
        this.findViewById<Button>(R.id.button).setOnClickListener(this)

        // Kiosk Mode
        KioskUtils(this).start(this)

        // WebView
        webView = findViewById(R.id.webView)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavaScriptInterface(this), "Android")


        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                super.shouldOverrideUrlLoading(view, request)
                view.loadUrl(request.url.toString())
                return true
            }
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

        val startUrl: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.START_URL).first().toString()
        }
        if (startUrl.isNotEmpty()) {
            webView.loadUrl(startUrl)
        } else {
            launchQRCodeActivity()
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        Log.d(tag,"onResume(): $ACTION_INVITE_ANSWERED")
        val filter = IntentFilter().apply {
            addAction(ACTION_INVITE_ANSWERED)
            addAction("com.example.ACTION_CALL_ENDED")
        }
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(tag, "onResume: BroadcastReceiver registered.")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
        Log.d(tag, "onPause: BroadcastReceiver unregistered.")
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(tag,"javascript:$intent")
            if (intent == null) return
            when (intent.action) {
                ACTION_INVITE_ANSWERED -> {
                    Log.d(tag,"javascript:onInviteAnswered()")
                    webView.evaluateJavascript("javascript:onInviteAnswered()", null)
                }
                "com.example.ACTION_CALL_ENDED" -> {
                    val callId = intent.getStringExtra("callId") ?: ""
                    webView.evaluateJavascript("javascript:onCallEnded('$callId')", null)
                }
                // 他のイベントもここに追加可能
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
                    DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.PASSWORD).first().toString()
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
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.START_URL).first().toString()
        }
        webView.loadUrl(startUrl)
    }

    private fun navigateConfigUrl() {
        val startUrl: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.CONFIG_URL).first().toString()
        }
        webView.loadUrl(startUrl)
    }

    private fun launchQRCodeActivity() {
        val intent = Intent(this, QRCodeScannerActivity::class.java)
        launcher.launch(intent)
    }

}
