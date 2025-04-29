package com.sozonext.inntouch.application

import android.app.Application
import com.portsip.PortSipSdk

class MyApplication : Application() {

    lateinit var portSipSdk: PortSipSdk
        private set

    var mConference: Boolean = false
    var mUseFrontCamera: Boolean = false

    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        portSipSdk = PortSipSdk(this)
    }

}