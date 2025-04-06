package com.sozonext.starryapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import com.sozonext.starryapp.utils.DataStoreUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SystemSettingsFragment : Fragment() {

    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        listView = ListView(requireContext())
        runBlocking {
            val ds = DataStoreUtils(requireContext())
            val data = listOf(
                mapOf(
                    "key" to "PASSWORD",
                    "value" to ds.getDataStoreValue(DataStoreUtils.PASSWORD).first().toString(),
                ),
            )
            listView.adapter = SimpleAdapter(
                requireContext(),
                data,
                android.R.layout.simple_list_item_2,
                arrayOf("key", "value"),
                intArrayOf(android.R.id.text1, android.R.id.text2)
            )
        }
        listView.setOnItemClickListener { parent, view, position, id ->
            if (position == 0) {
                runBlocking {
                    DataStoreUtils(requireContext()).resetDataStore()
                }
            }
        }
        return listView
    }

}
