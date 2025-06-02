package com.sozonext.inntouch.ui.fragment

import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sozonext.inntouch.R
import com.sozonext.inntouch.utils.KioskUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
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
        val apkDownloadInfoUrl = getString(R.string.latest_apk_version_url)
        val client = OkHttpClient()
        val request = Request.Builder().url(apkDownloadInfoUrl).build()

        KioskUtil(context).stop(requireActivity())

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "アップデート情報の取得に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val apkUrl = JSONObject(body).getString("url")
                downloadAndPromptInstall(apkUrl)
            }
        })
    }

    private fun downloadAndPromptInstall(apkUrl: String) {
        val context = requireContext()
        val apkFile = File(context.getExternalFilesDir(null), "inntouch-latest-debug.apk")

        val request = Request.Builder().url(apkUrl).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "APKダウンロードに失敗しました", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val sink = apkFile.sink().buffer()
                sink.writeAll(response.body!!.source())
                sink.close()

                requireActivity().runOnUiThread {
                    promptInstall(apkFile)
                }
            }
        })
    }

    private fun promptInstall(apkFile: File) {
        val context = requireContext()
        val apkUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

}