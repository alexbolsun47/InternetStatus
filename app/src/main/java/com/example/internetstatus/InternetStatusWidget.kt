package com.example.internetstatus

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InternetStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val repository =
            StatusRepository(
                context.applicationContext
            )

        val savedStatus =
            repository.status.first()

        provideContent {
            val currentStatus by repository.status.collectAsState(initial = savedStatus)
            WidgetContent(
                savedStatus = currentStatus
            )
        }
    }
}

@Composable
private fun WidgetContent(
    savedStatus: SavedNetworkStatus
) {
    val status =
        savedStatus.mobileStatus

    val statusColor =
        when (status) {

            InternetStatus.FULL ->
                Color(0xFF2E7D32)

            InternetStatus.WHITELIST ->
                Color(0xFFF9A825)

            InternetStatus.NO_INTERNET ->
                Color(0xFFC62828)

            InternetStatus.CHECKING ->
                Color(0xFF757575)

            InternetStatus.NETWORK_UNAVAILABLE,
            InternetStatus.MOBILE_DATA_DISABLED,
            InternetStatus.WIFI_DISABLED,
            InternetStatus.NOT_CHECKED ->
                Color(0xFF757575)
        }

    val shortStatus =
        when (status) {

            InternetStatus.FULL ->
                "Полный интернет"

            InternetStatus.WHITELIST ->
                "Белый список"

            InternetStatus.NO_INTERNET ->
                "Нет доступа"

            InternetStatus.NETWORK_UNAVAILABLE ->
                "Сеть недоступна"

            InternetStatus.MOBILE_DATA_DISABLED ->
                "Моб. данные выкл."

            InternetStatus.WIFI_DISABLED ->
                "Wi-Fi выключен"

            InternetStatus.CHECKING ->
                "Проверяем..."

            InternetStatus.NOT_CHECKED ->
                "Не проверено"
        }

    val lastCheckText =
        if (savedStatus.lastCheckTime > 0L) {

            val formatter =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            formatter.format(
                Date(savedStatus.lastCheckTime)
            )

        } else {
            "—"
        }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {

        // Верхняя строка: название + обновление
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Мобильная сеть",
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(
                    fontWeight = FontWeight.Bold
                )
            )

            WidgetRefreshButton(checking = status == InternetStatus.CHECKING)
        }

        Spacer(
            modifier = GlanceModifier.size(1.dp)
        )

        // Цветная плашка статуса
        Row {

            Text(
                text = shortStatus,
                maxLines = 1,
                modifier = GlanceModifier
                    .background(
                        imageProvider = ImageProvider(R.drawable.widget_status_background),
                        colorFilter = ColorFilter.tint(
                            ColorProvider(
                                day = statusColor,
                                night = statusColor
                            )
                        )
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 3.dp
                    ),
                style = TextStyle(
                    color = ColorProvider(
                        day = Color.White,
                        night = Color.White
                    ),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(
            modifier = GlanceModifier.size(9.dp)
        )

        Text(
            text = "Проверено: $lastCheckText",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(
                    day = Color.Gray,
                    night = Color.LightGray
                )
            )
        )
    }
}

@Composable
private fun WidgetRefreshButton(checking: Boolean) {
    val context = LocalContext.current
    val buttonDescription = context.getString(
        if (checking) R.string.widget_refresh_checking else R.string.widget_refresh
    )
    val clickModifier = if (checking) {
        GlanceModifier
    } else {
        GlanceModifier.clickable(
            onClick = actionRunCallback<RefreshWidgetAction>(),
            rippleOverride = R.drawable.widget_refresh_ripple
        )
    }

    // The visible button is 40 dp; keep the larger touch target around it.
    Box(
        modifier = GlanceModifier
            .size(48.dp)
            .then(clickModifier)
            .semantics { contentDescription = buttonDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .background(ImageProvider(R.drawable.widget_refresh_background)),
            contentAlignment = Alignment.Center
        ) {
            if (checking) {
                CircularProgressIndicator(
                    modifier = GlanceModifier.size(20.dp),
                    color = ColorProvider(
                        day = Color(0xFF63DDD9),
                        night = Color(0xFF63DDD9)
                    )
                )
            } else {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = null,
                    modifier = GlanceModifier.size(26.dp)
                )
            }
        }
    }
}

class InternetStatusWidgetReceiver :
    GlanceAppWidgetReceiver() {

    override val glanceAppWidget:
            GlanceAppWidget =
        InternetStatusWidget()
}
