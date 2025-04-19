package com.sozonext.inn_touch.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
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
import androidx.core.content.ContextCompat
import com.sozonext.inn_touch.R
import com.sozonext.inn_touch.utils.DataStoreUtils
import com.sozonext.inn_touch.utils.KioskUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var webView: WebView

    private var counter = 0
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        this.findViewById<Button>(R.id.button).setOnClickListener(this)

        // Kiosk Mode
        KioskUtils(this).start(this)

        // WebView
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                webView.addView(
                    view, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                // カメラやマイク使用要求に自動で許可
                request.grant(request.resources)
            }
            override fun onHideCustomView() {
                super.onHideCustomView()
                webView.removeAllViews()
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.cancel()
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
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

        requestPermissions()
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
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("アプリ設定")
            .setMessage("パスワードを入力してください。")
            .setView(dialogEditPassword)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> }
            .setNegativeButton("キャンセル") { _, _ -> }
            .create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val inputPassword =
                    dialogEditPassword.findViewById<EditText>(R.id.editTextPassword).text.toString()
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

    private val REQUEST_CODE_PERMISSIONS = 123
    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }
}
