package com.example.internetstatus

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.flow.first

class RefreshWidgetAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {

        val appContext =
            context.applicationContext

        val statusRepository =
            StatusRepository(appContext)

        val previous =
            statusRepository.status.first()

        /*
         * Сразу даём пользователю реакцию.
         */
        statusRepository.saveStatus(
            mobileStatus =
                InternetStatus.CHECKING,

            wifiStatus =
                previous.wifiStatus,

            mobileDetails =
                "Проверяем мобильную сеть...",

            wifiDetails =
                previous.wifiDetails,

            /*
             * Старое время сохраняем,
             * потому что новая проверка
             * ещё не закончилась.
             */
            lastCheckTime =
                previous.lastCheckTime
        )

        /*
         * Сеть внутри ActionCallback
         * больше НЕ проверяем.
         *
         * Отдаём работу WorkManager.
         */
        BackgroundCheckScheduler.runMobileNow(
            appContext
        )
    }
}