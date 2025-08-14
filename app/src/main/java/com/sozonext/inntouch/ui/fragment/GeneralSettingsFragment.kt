package com.sozonext.inntouch.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import com.sozonext.inntouch.utils.KioskUtil

class GeneralSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val listView = ListView(context)
        val data = listOf(
            mapOf(
                "key" to "客室の設定をする",
            ),
            mapOf(
                "key" to "アプリの固定を解除する",
            ),
            mapOf(
                "key" to "キャッシュを削除する",
            ),
        )
        listView.adapter = SimpleAdapter(
            context,
            data,
            android.R.layout.simple_list_item_1,
            arrayOf("key"),
            intArrayOf(android.R.id.text1)
        )
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                // 客室の設定をする
                0 -> navigateConfigUrl()
                // アプリの固定を解除する
                1 -> stopKioskMode()
                // キャッシュを削除する
                2 -> clearCache()
            }
        }
        return listView
    }

    private fun navigateConfigUrl() {
        val intent = Intent()
        intent.putExtra("event", "navigateConfigUrl")
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    private fun stopKioskMode() {
        KioskUtil(requireContext()).stop(requireActivity())
    }

    private fun clearCache() {
        val intent = Intent()
        intent.putExtra("event", "clearCache")
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

}
