package com.example.internetstatus

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import java.util.concurrent.TimeUnit

object BackgroundCheckScheduler {

    private const val PERIODIC_WORK_NAME =
        "internet_status_periodic_check"

    /*
     * Обычная фоновая проверка
     * каждые 15 минут.
     *
     * Проверяются:
     * - мобильная сеть
     * - Wi-Fi
     */
    fun schedulePeriodic(
        context: Context
    ) {

        val request =
            PeriodicWorkRequestBuilder<
                    InternetCheckWorker
                    >(
                15,
                TimeUnit.MINUTES
            )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    /*
     * Тестовая ручная проверка
     * через WorkManager.
     *
     * Проверяет обе сети.
     */
    fun runNow(
        context: Context
    ) {

        val request =
            OneTimeWorkRequestBuilder<
                    InternetCheckWorker
                    >()
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(request)
    }

    /*
     * Ручное обновление из виджета.
     *
     * Проверяем ТОЛЬКО мобильную сеть,
     * потому что виджет показывает именно её.
     */
    suspend fun runMobileNow(
        context: Context
    ) {

        val inputData =
            Data.Builder()
                .putBoolean(
                    "mobile_only",
                    true
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<
                    InternetCheckWorker
                    >()
                .setInputData(inputData)
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(request)
            .await()
    }
}
