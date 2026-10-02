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

            val settingsRepository =
                SettingsRepository(
                    applicationContext
                )

            val statusRepository =
                StatusRepository(
                    applicationContext
                )

            try {

                val settings =
                    settingsRepository
                        .settings
                        .first()

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

                /*
                 * После успешной проверки
                 * обязательно обновляем виджет.
                 */
                updateInternetStatusWidget(
                    applicationContext
                )

                Result.success()

            } catch (e: Exception) {

                /*
                 * Если Worker упал,
                 * не оставляем виджет
                 * навечно в CHECKING.
                 */

                val previous =
                    statusRepository
                        .status
                        .first()

                statusRepository.saveStatus(

                    mobileStatus =
                        InternetStatus.NETWORK_UNAVAILABLE,

                    wifiStatus =
                        previous.wifiStatus,

                    mobileDetails =
                        "Ошибка фоновой проверки: " +
                                (e.message
                                    ?: e.javaClass.simpleName),

                    wifiDetails =
                        previous.wifiDetails,

                    lastCheckTime =
                        System.currentTimeMillis()
                )

                updateInternetStatusWidget(
                    applicationContext
                )

                Result.failure()
            }
        }
}