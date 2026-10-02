package com.example.internetstatus

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackgroundCheckScheduler {

    private const val PERIODIC_WORK_NAME =
        "internet_status_periodic_check"

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
     * Эта функция понадобится нам для тестирования.
     * Не надо ждать 15 минут.
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
}