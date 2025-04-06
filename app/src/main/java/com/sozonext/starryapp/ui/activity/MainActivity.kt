package com.sozonext.starryapp.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sozonext.starryapp.utils.DataStoreUtils
import com.sozonext.starryapp.utils.KioskUtils
import com.sozonext.starryapp.R
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

        val startUrl: String = runBlocking {
            DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.START_URL).first()
                .toString()
        }
        if (startUrl.isNotEmpty()) {
            webView.loadUrl(startUrl)
        } else {
            val intent = Intent(this, QRCodeScannerActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onClick(v: View?) {
        if (counter == 0) {
            handler.postDelayed({ counter = 0 }, 5 * 1000)
        }
        if (++counter >= 5) {
            counter = 0
            // runBlocking {DataStoreUtils(applicationContext).resetDataStore()}
            // KioskUtils(this).stop(this)
            openPasswordDialog()
        }
    }

    private fun openPasswordDialog() {
        val dialogEditPassword = layoutInflater.inflate(R.layout.dialog_edit_password, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("設定画面")
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
                    DataStoreUtils(applicationContext).getDataStoreValue(DataStoreUtils.PASSWORD)
                        .first().toString()
                }
                if (inputPassword == password) {
                    startActivity(Intent(this, MenuActivity::class.java))
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "パスワードが間違っています", Toast.LENGTH_SHORT).show()
                }
            }
        }
        alertDialog.show()
    }

}