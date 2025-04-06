package com.sozonext.starryapp.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.sozonext.starryapp.utils.DataStoreUtils
import kotlinx.coroutines.runBlocking


class SystemSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val listView = ListView(context)
        runBlocking {
            val data = listOf(
                mapOf(
                    "key" to "アプリの設定を初期化する (出荷状態に戻す)",
                ),
            )
            listView.adapter = SimpleAdapter(
                context,
                data,
                android.R.layout.simple_list_item_1,
                arrayOf("key"),
                intArrayOf(android.R.id.text1)
            )
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                // アプリの設定を初期化する
                0 -> factoryReset()
            }
        }
        return listView
    }

    private fun factoryReset() {
        val context = requireContext()
        AlertDialog.Builder(context).setTitle("警告")
            .setMessage("アプリの設定を初期化しようとしてます。\r\n再設定を行うには管理画面からQRコードを読み取る必要があります。\r\n本当によろしいでしょうか？")
            .setPositiveButton("OK") { _, _ ->
                runBlocking {
                    DataStoreUtils(context).resetDataStore()
                }
                val intent = Intent()
                intent.putExtra("event", "launchQRCodeActivity")
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            }
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

}
