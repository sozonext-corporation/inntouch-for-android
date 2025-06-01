package com.sozonext.inntouch.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.sozonext.inntouch.R
import com.sozonext.inntouch.ui.fragment.ApplicationUpdateFragment
import com.sozonext.inntouch.ui.fragment.SystemInformationFragment
import com.sozonext.inntouch.ui.fragment.GeneralSettingsFragment
import com.sozonext.inntouch.ui.fragment.SystemSettingsFragment

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("基本設定"))
        tabLayout.addTab(tabLayout.newTab().setText("アップデート"))
        tabLayout.addTab(tabLayout.newTab().setText("システム情報"))
        tabLayout.addTab(tabLayout.newTab().setText("システム設定"))

        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, GeneralSettingsFragment())
            .commit()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    // 基本設定
                    0 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, GeneralSettingsFragment())
                        .commit()
                    // アップデート
                    1 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ApplicationUpdateFragment())
                        .commit()
                    // システム情報
                    2 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, SystemInformationFragment())
                        .commit()
                    // システム設定
                    3 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, SystemSettingsFragment())
                        .commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

    }

}
