package com.sozonext.inntouch.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.sozonext.inntouch.R
import com.sozonext.inntouch.utils.DataStoreUtils
import com.sozonext.inntouch.utils.KioskUtil
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
                    "key" to "アプリを初期化する (QRコードの再読み込みが必要)",
                ),
                mapOf(
                    "key" to "デバイス管理アプリを無効にする",
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
                // ソフトウェアアップデート (今すぐ)"
                0 -> resetApplication()
                // デバイス管理アプリを無効にする
                1 -> removeDeviceOwner()
            }
        }
        return listView
    }

    private fun resetApplication() {
        val context = requireContext()
        AlertDialog.Builder(context).setTitle("警告")
            .setMessage("アプリを初期化しようとしてます。\r\n再度登録を行うには管理画面からQRコードを読み取る必要があります。\r\n本当によろしいでしょうか？")
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

    private fun removeDeviceOwner() {
        val context = requireContext()
        AlertDialog.Builder(context).setTitle("警告")
            .setMessage("デバイス管理アプリを無効にしようとしてます。\r\n本当によろしいでしょうか？")
            .setPositiveButton("OK") { _, _ ->
                val dialogEditPassword = layoutInflater.inflate(R.layout.dialog_edit_password, null)
                val alertDialog = AlertDialog.Builder(context).setTitle("アプリ設定").setMessage("パスワードを入力してください。").setView(dialogEditPassword).setCancelable(false).setPositiveButton("OK") { _, _ -> }.setNegativeButton("キャンセル") { _, _ -> }.create()
                alertDialog.setOnShowListener {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val inputPassword = dialogEditPassword.findViewById<EditText>(R.id.editTextPassword).text.toString()
                        if (inputPassword == "1234567890") {
                            KioskUtil(context).removeDeviceOwner()
                        } else {
                            Toast.makeText(context, "パスワードが間違っています", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                alertDialog.show()
            }
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

}
