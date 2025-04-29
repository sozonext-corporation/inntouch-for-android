package com.sozonext.inntouch.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.sozonext.inntouch.application.MyApplication
import com.sozonext.inntouch.service.PortSipService
import com.sozonext.inntouch.utils.DataStoreUtils
import com.sozonext.inntouch.utils.portsip.CallManager
import kotlinx.coroutines.runBlocking

class JavaScriptInterface(private val context: Context) {

    private val tag: String = JavaScriptInterface::class.java.simpleName

    @JavascriptInterface
    fun register(sipServer: String, sipDomain: String, extensionNumber: String, extensionPassword: String): Boolean {
        Log.d(tag, "register($sipServer, $sipDomain, $extensionNumber, $extensionPassword)")

        runBlocking {
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.SIP_SERVER, sipServer)
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.SIP_DOMAIN, sipDomain)
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.EXTENSION_NUMBER, extensionNumber)
            DataStoreUtils(context).setDataStoreValue(DataStoreUtils.EXTENSION_PASSWORD, extensionPassword)
        }

        if (CallManager.instance().isOnline) {
            Toast.makeText(context, "Please OffLine First...", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(context, PortSipService::class.java).apply {
                action = PortSipService.ACTION_SIP_REGISTER
            }
            PortSipService.startServiceCompatibility(context, intent)
        }
        return true
    }

    @JavascriptInterface
    fun unregister(): Boolean {
        Log.d(tag, "unregister()")
        val intent = Intent(context, PortSipService::class.java).apply {
            action = PortSipService.ACTION_SIP_UNREGISTER
        }
        PortSipService.startServiceCompatibility(context, intent)
        return true
    }

    @JavascriptInterface
    fun call(extensionNumber: String): Boolean {
        val portSipSdk = MyApplication.instance.portSipSdk
        Log.d(tag, "call($extensionNumber)")
        val n = "801@its2.sozonext.com"
        val sessionId = portSipSdk.call(n, true, false)
        if (sessionId <= 0) {
            Toast.makeText(context, "Call failure", Toast.LENGTH_SHORT).show()
            return false
        }
        portSipSdk.sendVideo(sessionId, true)
        return true
    }

    @JavascriptInterface
    fun answer(): Boolean {
        Log.d(tag, "answer()")
        return true
    }

    @JavascriptInterface
    fun hangUp(): Boolean {
        Log.d(tag, "hangUp()")
        return true
    }

    @JavascriptInterface
    fun mute(enable: Boolean): Boolean {
        Log.d(tag, "mute($enable)")
        return true
    }

    @JavascriptInterface
    fun hold(enable: Boolean): Boolean {
        Log.d(tag, "hold($enable)")
        return true
    }

}