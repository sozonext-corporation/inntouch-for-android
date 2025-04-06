package com.sozonext.starryapp.ui.activity

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.sozonext.starryapp.utils.DataStoreUtils
import com.sozonext.starryapp.R
import com.sozonext.starryapp.ui.fragment.GeneralSettingsFragment
import com.sozonext.starryapp.ui.fragment.InformationFragment
import com.sozonext.starryapp.ui.fragment.SystemSettingsFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MenuActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var frameLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("基本設定"))
        tabLayout.addTab(tabLayout.newTab().setText("アプリ情報"))
        tabLayout.addTab(tabLayout.newTab().setText("システム設定"))

        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, GeneralSettingsFragment())
            .commit()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, GeneralSettingsFragment())
                        .commit()

                    1 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, InformationFragment())
                        .commit()

                    2 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, SystemSettingsFragment())
                        .commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val listView: ListView = findViewById(R.id.listView)
        runBlocking {
            val dataStoreUtils = DataStoreUtils(applicationContext)
            val settingsList = listOf(
                Pair(
                    "パスワード",
                    dataStoreUtils.getDataStoreValue(DataStoreUtils.PASSWORD).first().toString()
                ),
                Pair(
                    "起動 URL",
                    dataStoreUtils.getDataStoreValue(DataStoreUtils.START_URL).first().toString()
                ),
                Pair(
                    "設定 URL",
                    dataStoreUtils.getDataStoreValue(DataStoreUtils.CONFIG_URL).first().toString()
                ),
            )
        }

    }

}

