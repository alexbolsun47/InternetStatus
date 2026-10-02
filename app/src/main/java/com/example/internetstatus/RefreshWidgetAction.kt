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

        val repository =
            StatusRepository(
                context.applicationContext
            )

        val currentStatus =
            repository.status.first()

        /*
         * Сразу показываем на виджете,
         * что проверка запущена.
         */
        repository.saveStatus(
            mobileStatus =
                InternetStatus.CHECKING,

            wifiStatus =
                currentStatus.wifiStatus,

            mobileDetails =
                "Выполняется проверка...",

            wifiDetails =
                currentStatus.wifiDetails,

            lastCheckTime =
                currentStatus.lastCheckTime
        )

        updateInternetStatusWidget(
            context.applicationContext
        )

        /*
         * После этого запускаем Worker.
         * Когда он закончит, сохранит
         * настоящий результат и снова
         * обновит виджет.
         */
        BackgroundCheckScheduler.runNow(
            context.applicationContext
        )
    }
}