package com.example.internetstatus

import android.content.Context
import androidx.glance.appwidget.updateAll

suspend fun updateInternetStatusWidget(
    context: Context
) {
    InternetStatusWidget().updateAll(context)
}