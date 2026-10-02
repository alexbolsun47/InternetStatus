package com.example.internetstatus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class InternetCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result =
        coroutineScope {

            try {

                val settingsRepository =
                    SettingsRepository(
                        applicationContext
                    )

                val statusRepository =
                    StatusRepository(
                        applicationContext
                    )

                /*
                 * Берём актуальные пользовательские
                 * настройки из DataStore.
                 */
                val settings =
                    settingsRepository
                        .settings
                        .first()

                /*
                 * Wi-Fi и мобильную сеть
                 * проверяем параллельно.
                 */
                val mobileDeferred =
                    async {

                        checkNetwork(
                            context =
                                applicationContext,

                            networkType =
                                NetworkType.MOBILE,

                            settings =
                                settings
                        )
                    }

                val wifiDeferred =
                    async {

                        checkNetwork(
                            context =
                                applicationContext,

                            networkType =
                                NetworkType.WIFI,

                            settings =
                                settings
                        )
                    }

                val mobile =
                    mobileDeferred.await()

                val wifi =
                    wifiDeferred.await()

                /*
                 * Сохраняем результат.
                 */
                statusRepository.saveStatus(

                    mobileStatus =
                        mobile.status,

                    wifiStatus =
                        wifi.status,

                    mobileDetails =
                        mobile.details,

                    wifiDetails =
                        wifi.details,

                    lastCheckTime =
                        System.currentTimeMillis()
                )

                Result.success()

            } catch (e: Exception) {

                /*
                 * Не хотим убивать периодическую задачу
                 * из-за единичной ошибки.
                 */
                Result.retry()
            }
        }
}