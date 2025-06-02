package com.sozonext.inntouch.application

import android.app.Application
import com.portsip.PortSipSdk

class MyApplication : Application() {

    companion object {
        lateinit var portSipSdk: PortSipSdk
            private set
    }

    override fun onCreate() {
        super.onCreate()
        portSipSdk = PortSipSdk(this)
    }

}