package com.example.internetstatus

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class InternetCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val statusRepository = StatusRepository(applicationContext)

        return try {
            val settings = settingsRepository.settings.first()
            runNetworkChecks(
                mobileOnly = inputData.getBoolean("mobile_only", false),
                check = { type -> checkNetwork(applicationContext, type, settings) },
                onResult = { type, result ->
                    when (type) {
                        NetworkType.MOBILE -> statusRepository.saveStatus(
                            mobileStatus = result.status,
                            mobileDetails = result.details,
                            lastCheckTime = System.currentTimeMillis()
                        )
                        NetworkType.WIFI -> statusRepository.saveStatus(
                            wifiStatus = result.status,
                            wifiDetails = result.details
                        )
                    }
                }
            )
            Result.success()
        } catch (e: CancellationException) {
            finishChecking(statusRepository, "Проверка отменена.")
            throw e
        } catch (e: Exception) {
            finishChecking(
                statusRepository,
                "Ошибка фоновой проверки: ${e.message ?: e.javaClass.simpleName}"
            )
            Result.failure()
        }
    }

    private suspend fun finishChecking(repository: StatusRepository, details: String) {
        withContext(NonCancellable) {
            try {
                repository.finishChecking(details)
            } catch (e: Exception) {
                Log.e("InternetCheckWorker", "Не удалось сохранить ошибку проверки", e)
            }
        }
    }
}
