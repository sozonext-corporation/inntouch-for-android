package com.sozonext.starryapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import com.sozonext.starryapp.utils.KioskUtils

class GeneralSettingsFragment : Fragment() {

    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        listView = ListView(requireContext())
        val list = listOf(
            mapOf(
                "key" to "アプリの固定を解除する",
            ),
            mapOf(
                "key" to "客室設定を変更する",
            ),
        )
        listView.adapter = SimpleAdapter(
            requireContext(),
            list,
            android.R.layout.simple_list_item_1,
            arrayOf("key"),
            intArrayOf(android.R.id.text1)
        )
        listView.setOnItemClickListener { _, _, position, _ ->
            // アプリの固定を解除する
            if (position == 0) {
                KioskUtils(requireContext()).stop(requireActivity())
            } else if (position == 1) {
                KioskUtils(requireContext()).stop(requireActivity())
            }
        }
        return listView
    }

}
