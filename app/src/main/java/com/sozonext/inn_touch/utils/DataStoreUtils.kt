package com.sozonext.inn_touch.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreUtils(private val context: Context) {

    private val tag: String = AppCompatActivity::class.java.simpleName

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        val PASSWORD = stringPreferencesKey("password")
        val START_URL = stringPreferencesKey("start_url")
        val CONFIG_URL = stringPreferencesKey("config_url")
        val SIP_SERVER = stringPreferencesKey("sip_server")
        val SIP_DOMAIN = stringPreferencesKey("sip_domain")
        val EXTENSION_NUMBER = stringPreferencesKey("extension_number")
        val EXTENSION_PASSWORD = stringPreferencesKey("extension_password")
        const val PASSWORD_DEFAULT = "0000"
        const val START_URL_DEFAULT = ""
        const val CONFIG_URL_DEFAULT = ""
        const val SIP_SERVER_DEFAULT = ""
        const val SIP_DOMAIN_DEFAULT = ""
        const val EXTENSION_NUMBER_DEFAULT = ""
        const val EXTENSION_PASSWORD_DEFAULT = ""
    }

    suspend fun setDataStoreValue(key: Preferences.Key<String>, value: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            Log.e(tag, "Exception: $e")
        }
    }

    fun getDataStoreValue(key: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[key].orEmpty()
        }
    }

    suspend fun resetDataStore() {
        setDataStoreValue(PASSWORD, PASSWORD_DEFAULT)
        setDataStoreValue(START_URL, START_URL_DEFAULT)
        setDataStoreValue(CONFIG_URL, CONFIG_URL_DEFAULT)
        setDataStoreValue(SIP_SERVER, SIP_SERVER_DEFAULT)
        setDataStoreValue(SIP_DOMAIN, SIP_DOMAIN_DEFAULT)
        setDataStoreValue(EXTENSION_NUMBER, EXTENSION_NUMBER_DEFAULT)
        setDataStoreValue(EXTENSION_PASSWORD, EXTENSION_PASSWORD_DEFAULT)
    }

}