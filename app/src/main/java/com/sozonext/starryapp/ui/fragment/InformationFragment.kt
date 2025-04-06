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

class InformationFragment : Fragment() {

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
                mapOf(
                    "key" to "START_URL",
                    "value" to ds.getDataStoreValue(DataStoreUtils.START_URL).first().toString(),
                ),
                mapOf(
                    "key" to "CONFIG_URL",
                    "value" to ds.getDataStoreValue(DataStoreUtils.CONFIG_URL).first().toString(),
                ),
                mapOf(
                    "key" to "SIP_SERVER",
                    "value" to ds.getDataStoreValue(DataStoreUtils.SIP_SERVER).first().toString(),
                ),
                mapOf(
                    "key" to "SIP_DOMAIN",
                    "value" to ds.getDataStoreValue(DataStoreUtils.SIP_DOMAIN).first().toString(),
                ),
                mapOf(
                    "key" to "EXTENSION_NUMBER",
                    "value" to ds.getDataStoreValue(DataStoreUtils.EXTENSION_NUMBER).first()
                        .toString(),
                ),
                mapOf(
                    "key" to "EXTENSION_PASSWORD",
                    "value" to ds.getDataStoreValue(DataStoreUtils.EXTENSION_PASSWORD).first()
                        .toString(),
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
        return listView
    }

}
