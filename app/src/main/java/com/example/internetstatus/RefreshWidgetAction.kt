package com.example.internetstatus

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class RefreshWidgetAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val repository = StatusRepository(appContext)

        // Finish handing off to WorkManager even if the widget callback is cancelled.
        // Only storage and enqueueing happen here; network checks remain in the Worker.
        withContext(NonCancellable) {
            try {
                if (!repository.tryStartMobileCheck()) return@withContext
                BackgroundCheckScheduler.runMobileNow(appContext)
            } catch (e: Exception) {
                Log.e("RefreshWidgetAction", "Не удалось запустить проверку", e)
                repository.finishChecking(
                    "Не удалось запустить проверку: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }
}
