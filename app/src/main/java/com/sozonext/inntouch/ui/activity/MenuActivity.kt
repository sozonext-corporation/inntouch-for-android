package com.sozonext.inntouch.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.sozonext.inntouch.R
import com.sozonext.inntouch.ui.fragment.ApplicationUpdateFragment
import com.sozonext.inntouch.ui.fragment.GeneralSettingsFragment
import com.sozonext.inntouch.ui.fragment.SystemInformationFragment
import com.sozonext.inntouch.ui.fragment.SystemSettingsFragment

class MenuActivity : AppCompatActivity() {

    private val tag = this::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // ツールバーに戻るボタンを追加
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        // 基本設定を初期表示する
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, GeneralSettingsFragment())
            .commit()

        // タブレイアウトの項目を追加する
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("基本設定"))
        tabLayout.addTab(tabLayout.newTab().setText("アップデート"))
        tabLayout.addTab(tabLayout.newTab().setText("システム情報"))
        tabLayout.addTab(tabLayout.newTab().setText("システム設定"))

        // タブレイアウトの切り替えイベントを追加をする
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                lateinit var fragment: Fragment
                when (tab.position) {
                    // 基本設定
                    0 -> fragment = GeneralSettingsFragment()
                    // アップデート
                    1 -> fragment = ApplicationUpdateFragment()
                    // システム情報
                    2 -> fragment = SystemInformationFragment()
                    // システム設定
                    3 -> fragment = SystemSettingsFragment()
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
