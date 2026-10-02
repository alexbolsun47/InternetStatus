package com.example.internetstatus

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        mobileStatus: InternetStatus,
        wifiStatus: InternetStatus,
        mobileDetails: String,
        wifiDetails: String,
        lastCheckTime: Long
    ) {

        context.statusDataStore.edit { preferences ->

            preferences[Keys.MOBILE_STATUS] =
                mobileStatus.name

            preferences[Keys.WIFI_STATUS] =
                wifiStatus.name

            preferences[Keys.MOBILE_DETAILS] =
                mobileDetails

            preferences[Keys.WIFI_DETAILS] =
                wifiDetails

            preferences[Keys.LAST_CHECK_TIME] =
                lastCheckTime
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