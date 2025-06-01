package com.sozonext.inntouch.ui.fragment

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.sozonext.inntouch.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException

class ApplicationUpdateFragment : Fragment() {

    private lateinit var adapter: SimpleAdapter
    private lateinit var adapterData: MutableList<Map<String, String>>

    private var downloadId: Long = 0L
    private var downloadReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val listView = ListView(context)

        val packageInfo = context.packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionCode = packageInfo.longVersionCode
        val versionName = packageInfo.versionName

        adapterData = mutableListOf(
            mapOf(
                "key" to "現在のバージョン",
                "value" to "$versionName ($versionCode)",
            ),
            mapOf(
                "key" to "最新のバージョン",
                "value" to "取得中...",
            ),
            mapOf(
                "key" to "最新のバージョンにアップデートをする",
                "value" to "実施時間: 1分～5分程",
            ),
        )

        adapter = SimpleAdapter(
            context,
            adapterData,
            android.R.layout.simple_list_item_2,
            arrayOf("key", "value"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                // 最新のバージョン
                1 -> checkLatestVersion()
                // 最新のバージョンにアップデートをする
                2 -> installLatestVersion()
            }
        }

        // 最新のバージョンの取得
        checkLatestVersion()

        return listView
    }

    private fun checkLatestVersion() {
        val client = OkHttpClient()
        val request = Request.Builder().url(getString(R.string.latest_apk_version_url)).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "最新のバージョンの取得に失敗しました。", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val json = JSONObject(it)
                    val versionCode = json.getLong("versionCode")
                    val versionName = json.getString("versionName")
                    requireActivity().runOnUiThread {
                        adapterData[1] = mapOf("key" to "最新のバージョン", "value" to "$versionName ($versionCode)")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun installLatestVersion() {
        val context = requireContext()

        // Android 8.0以上で不明ソース許可チェック
        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(context, "不明なアプリのインストールを許可してください", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
            }
            startActivity(intent)
            return
        }

        val request = DownloadManager.Request(getString(R.string.latest_apk_url).toUri()).apply {
            setTitle("アップデートのダウンロード中")
            setDescription("最新バージョンのアプリをダウンロードしています")
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "inntouch.apk")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType("application/vnd.android.package-archive")
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        if (downloadReceiver != null) {
            context.unregisterReceiver(downloadReceiver)
        }

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    downloadReceiver?.let { context.unregisterReceiver(it) }
                    downloadReceiver = null

                    val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "inntouch.apk")
                    if (apkFile.exists()) {
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            apkFile
                        )
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        startActivity(installIntent)
                    } else {
                        Toast.makeText(context, "ダウンロードしたAPKが見つかりません", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Receiver登録。APIレベルによってRECEIVER_NOT_EXPORTEDフラグ設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(context, downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }
}