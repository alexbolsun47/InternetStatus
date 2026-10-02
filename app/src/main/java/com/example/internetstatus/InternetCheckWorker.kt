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

                val mobileOnly =
                    inputData.getBoolean(
                        "mobile_only",
                        false
                    )

                /*
                 * Ручное обновление виджета:
                 * проверяем только MOBILE.
                 */
                if (mobileOnly) {

                    val previous =
                        statusRepository
                            .status
                            .first()

                    val mobile =
                        checkNetwork(
                            context =
                                applicationContext,

                            networkType =
                                NetworkType.MOBILE,

                            settings =
                                settings
                        )

                    statusRepository.saveStatus(
                        mobileStatus =
                            mobile.status,

                        wifiStatus =
                            previous.wifiStatus,

                        mobileDetails =
                            mobile.details,

                        wifiDetails =
                            previous.wifiDetails,

                        lastCheckTime =
                            System.currentTimeMillis()
                    )

                    return@coroutineScope Result.success()
                }

                /*
                 * Обычная фоновая проверка:
                 * MOBILE + Wi-Fi.
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

                /*
                 * Mobile готов первым —
                 * сразу сохраняем.
                 */
                val mobile =
                    mobileDeferred.await()

                val previous =
                    statusRepository
                        .status
                        .first()

                val checkTime =
                    System.currentTimeMillis()

                statusRepository.saveStatus(
                    mobileStatus =
                        mobile.status,

                    wifiStatus =
                        previous.wifiStatus,

                    mobileDetails =
                        mobile.details,

                    wifiDetails =
                        previous.wifiDetails,

                    lastCheckTime =
                        checkTime
                )

                /*
                 * Wi-Fi завершается позже.
                 */
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
                        checkTime
                )

                Result.success()

            } catch (e: Exception) {

                val previous =
                    statusRepository
                        .status
                        .first()

                /*
                 * Никогда не оставляем CHECKING.
                 */
                statusRepository.saveStatus(
                    mobileStatus =
                        InternetStatus.NETWORK_UNAVAILABLE,

                    wifiStatus =
                        previous.wifiStatus,

                    mobileDetails =
                        "Ошибка фоновой проверки: " +
                                (
                                        e.message
                                            ?: e.javaClass.simpleName
                                        ),

                    wifiDetails =
                        previous.wifiDetails,

                    lastCheckTime =
                        System.currentTimeMillis()
                )

                Result.failure()
            }
        }
}