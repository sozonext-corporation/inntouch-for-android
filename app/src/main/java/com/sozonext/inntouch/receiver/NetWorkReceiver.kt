package com.sozonext.inntouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log

class NetWorkReceiver : BroadcastReceiver() {

    private val tag = this::class.java.simpleName

    private lateinit var listener: NetWorkListener

    override fun onReceive(context: Context?, intent: Intent) {
        Log.d(tag, "onReceive()")
        try {
            if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION && null != context) {
                val netWorkState: Int = getNetWorkState(context)
                listener.onNetworkChange(netWorkState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface NetWorkListener {
        fun onNetworkChange(netMobile: Int)
    }

    fun setListener(netWorkListener: NetWorkListener) {
        listener = netWorkListener
    }

    private fun getNetWorkState(context: Context): Int {

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = connectivityManager.allNetworks

        for (i in networks.indices) {
            val networkInfo = connectivityManager.getNetworkInfo(networks[i])
            if (networkInfo!!.isConnected) {
                return if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                    ConnectivityManager.TYPE_MOBILE
                } else {
                    ConnectivityManager.TYPE_WIFI
                }
            }
        }
        return -1
    }

}