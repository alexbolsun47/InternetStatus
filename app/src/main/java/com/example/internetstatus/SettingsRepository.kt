package com.example.internetstatus

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "internet_status_settings"
)

const val DEFAULT_TIMEOUT_MS = 2000

data class AppSettings(

    val fullInternetUrls: List<String> = listOf(
        "https://www.google.com"
    ),

    val whitelistUrls: List<String> = listOf(
        "https://yandex.ru"
    ),

    val timeoutMs: Int = DEFAULT_TIMEOUT_MS,

    val fullInternetThreshold: Int = 1,

    val whitelistThreshold: Int = 1
)

class SettingsRepository(
    private val context: Context
) {

    private object Keys {

        val FULL_URLS =
            stringSetPreferencesKey("full_urls")

        val WHITELIST_URLS =
            stringSetPreferencesKey("whitelist_urls")

        val TIMEOUT =
            intPreferencesKey("timeout_ms")

        val FULL_THRESHOLD =
            intPreferencesKey("full_threshold")

        val WHITELIST_THRESHOLD =
            intPreferencesKey("whitelist_threshold")
    }

    val settings: Flow<AppSettings> =
        context.dataStore.data.map { preferences ->

            AppSettings(

                fullInternetUrls =
                    preferences[Keys.FULL_URLS]
                        ?.toList()
                        ?.sorted()
                        ?: listOf(
                            "https://www.google.com"
                        ),

                whitelistUrls =
                    preferences[Keys.WHITELIST_URLS]
                        ?.toList()
                        ?.sorted()
                        ?: listOf(
                            "https://yandex.ru"
                        ),

                timeoutMs =
                    preferences[Keys.TIMEOUT]
                        ?: DEFAULT_TIMEOUT_MS,

                fullInternetThreshold =
                    preferences[Keys.FULL_THRESHOLD]
                        ?: 1,

                whitelistThreshold =
                    preferences[Keys.WHITELIST_THRESHOLD]
                        ?: 1
            )
        }

    suspend fun saveSettings(
        settings: AppSettings
    ) {

        context.dataStore.edit { preferences ->

            preferences[Keys.FULL_URLS] =
                settings.fullInternetUrls.toSet()

            preferences[Keys.WHITELIST_URLS] =
                settings.whitelistUrls.toSet()

            preferences[Keys.TIMEOUT] =
                settings.timeoutMs

            preferences[Keys.FULL_THRESHOLD] =
                settings.fullInternetThreshold

            preferences[Keys.WHITELIST_THRESHOLD] =
                settings.whitelistThreshold
        }
    }
}
