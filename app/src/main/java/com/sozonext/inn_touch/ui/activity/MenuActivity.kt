package com.sozonext.inn_touch.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.sozonext.inn_touch.R
import com.sozonext.inn_touch.ui.fragment.GeneralSettingsFragment
import com.sozonext.inn_touch.ui.fragment.AppInfoFragment
import com.sozonext.inn_touch.ui.fragment.SystemSettingsFragment

class MenuActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // 新規タブの追加
        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("基本設定"))
        tabLayout.addTab(tabLayout.newTab().setText("アプリ情報"))
        tabLayout.addTab(tabLayout.newTab().setText("システム設定"))

        // 初期表示
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, GeneralSettingsFragment())
            .commit()

        // TabLayoutの切り替え
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    // 基本設定
                    0 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, GeneralSettingsFragment())
                        .commit()
                    // アプリ情報
                    1 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, AppInfoFragment())
                        .commit()
                    // システム設定
                    2 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, SystemSettingsFragment())
                        .commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

    }

}
