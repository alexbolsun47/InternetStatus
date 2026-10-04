package com.example.internetstatus

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.statusDataStore: DataStore<Preferences>
        by preferencesDataStore(
            name = "internet_status_state"
        )

data class SavedNetworkStatus(
    val mobileStatus: InternetStatus = InternetStatus.NOT_CHECKED,
    val wifiStatus: InternetStatus = InternetStatus.NOT_CHECKED,
    val mobileDetails: String = "",
    val wifiDetails: String = "",
    val lastCheckTime: Long = 0L
)

class StatusRepository(
    private val context: Context
) {

    private object Keys {

        val MOBILE_STATUS =
            stringPreferencesKey("mobile_status")

        val WIFI_STATUS =
            stringPreferencesKey("wifi_status")

        val MOBILE_DETAILS =
            stringPreferencesKey("mobile_details")

        val WIFI_DETAILS =
            stringPreferencesKey("wifi_details")

        val LAST_CHECK_TIME =
            longPreferencesKey("last_check_time")
    }

    val status: Flow<SavedNetworkStatus> =
        context.statusDataStore.data.map { preferences ->

            SavedNetworkStatus(
                mobileStatus =
                    parseStatus(
                        preferences[Keys.MOBILE_STATUS]
                    ),

                wifiStatus =
                    parseStatus(
                        preferences[Keys.WIFI_STATUS]
                    ),

                mobileDetails =
                    preferences[Keys.MOBILE_DETAILS]
                        ?: "",

                wifiDetails =
                    preferences[Keys.WIFI_DETAILS]
                        ?: "",

                lastCheckTime =
                    preferences[Keys.LAST_CHECK_TIME]
                        ?: 0L
            )
        }

    suspend fun saveStatus(
        mobileStatus: InternetStatus? = null,
        wifiStatus: InternetStatus? = null,
        mobileDetails: String? = null,
        wifiDetails: String? = null,
        lastCheckTime: Long? = null
    ) {

        context.statusDataStore.edit { preferences ->

            // Update only the supplied transport; keep the other one's latest result.
            mobileStatus?.let { preferences[Keys.MOBILE_STATUS] = it.name }
            wifiStatus?.let { preferences[Keys.WIFI_STATUS] = it.name }
            mobileDetails?.let { preferences[Keys.MOBILE_DETAILS] = it }
            wifiDetails?.let { preferences[Keys.WIFI_DETAILS] = it }
            lastCheckTime?.let { preferences[Keys.LAST_CHECK_TIME] = it }
        }

        /*
         * Ключевое изменение:
         * любое изменение StatusRepository
         * автоматически перерисовывает виджет.
         */
        updateWidget()
    }

    suspend fun tryStartMobileCheck(): Boolean {
        var started = false
        context.statusDataStore.edit { preferences ->
            // Claim the check atomically, including taps received before the widget redraws.
            if (preferences[Keys.MOBILE_STATUS] != InternetStatus.CHECKING.name) {
                preferences[Keys.MOBILE_STATUS] = InternetStatus.CHECKING.name
                preferences[Keys.MOBILE_DETAILS] = "Проверяем мобильную сеть..."
                started = true
            }
        }
        if (started) updateWidget()
        return started
    }

    suspend fun finishChecking(details: String) {
        context.statusDataStore.edit { preferences ->
            // Do not replace a completed result if another check has already finished.
            if (preferences[Keys.MOBILE_STATUS] == InternetStatus.CHECKING.name) {
                preferences[Keys.MOBILE_STATUS] = InternetStatus.NETWORK_UNAVAILABLE.name
                preferences[Keys.MOBILE_DETAILS] = details
            }
        }
        updateWidget()
    }

    private suspend fun updateWidget() {
        try {
            InternetStatusWidget().updateAll(context.applicationContext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The saved result remains valid even when Glance cannot render it.
            Log.w("StatusRepository", "Не удалось обновить виджет", e)
        }
    }

    private fun parseStatus(
        value: String?
    ): InternetStatus {

        return try {

            if (value == null) {
                InternetStatus.NOT_CHECKED
            } else {
                InternetStatus.valueOf(value)
            }

        } catch (_: Exception) {

            InternetStatus.NOT_CHECKED
        }
    }
}
