package com.example.internetstatus

import android.content.Context
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)
        BackgroundCheckScheduler.schedulePeriodic(
            applicationContext
        )
        setContent {

            MaterialTheme {
                InternetStatusApp()
            }
        }
    }
}

enum class AppScreen {
    HOME,
    SETTINGS
}

enum class InternetStatus(
    val title: String,
    val color: Color
) {

    FULL(
        "Полный интернет",
        Color(0xFF2E7D32)
    ),

    WHITELIST(
        "Только белый список",
        Color(0xFFF9A825)
    ),

    NO_INTERNET(
        "Нет доступа",
        Color(0xFFC62828)
    ),

    NETWORK_UNAVAILABLE(
        "Не подключено",
        Color(0xFF757575)
    ),

    WIFI_DISABLED(
        "Wi-Fi выключен",
        Color(0xFF757575)
    ),

    MOBILE_DATA_DISABLED(
        "Мобильные данные выключены",
        Color(0xFF757575)
    ),
    CHECKING(
        "Проверяем...",
        Color(0xFF757575)
    ),
    NOT_CHECKED(
        "Не проверено",
        Color(0xFF757575)
    )
}

data class NetworkCheckSummary(
    val status: InternetStatus,
    val details: String
)

data class GroupCheckResult(
    val success: Boolean,
    val successCount: Int,
    val requiredCount: Int,
    val checkedCount: Int,
    val results: List<CheckResult>
)

@Composable
fun InternetStatusApp() {

    val context = LocalContext.current

    val repository = remember {

        SettingsRepository(
            context.applicationContext
        )
    }

    var screen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    when (screen) {

        AppScreen.HOME -> {

            HomeScreen(
                repository = repository,

                onOpenSettings = {
                    screen =
                        AppScreen.SETTINGS
                }
            )
        }

        AppScreen.SETTINGS -> {

            SettingsScreen(
                repository = repository,

                onBack = {
                    screen =
                        AppScreen.HOME
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    repository: SettingsRepository,
    onOpenSettings: () -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val statusRepository = remember {
        StatusRepository(
            context.applicationContext
        )
    }

    val savedStatus by statusRepository.status.collectAsState(
        initial = SavedNetworkStatus()
    )
    val settings by repository.settings.collectAsState(
        initial = AppSettings()
    )

    var mobileStatus by remember {
        mutableStateOf(
            InternetStatus.NOT_CHECKED
        )
    }

    var wifiStatus by remember {
        mutableStateOf(
            InternetStatus.NOT_CHECKED
        )
    }

    var mobileDetails by remember {
        mutableStateOf("")
    }

    var wifiDetails by remember {
        mutableStateOf("")
    }

    var lastCheck by remember {
        mutableStateOf("—")
    }

    var checking by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(savedStatus) {
        mobileStatus = savedStatus.mobileStatus
        wifiStatus = savedStatus.wifiStatus
        mobileDetails = savedStatus.mobileDetails
        wifiDetails = savedStatus.wifiDetails

        if (savedStatus.lastCheckTime > 0L) {

            val formatter =
                SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.getDefault()
                )

            lastCheck =
                formatter.format(
                    Date(
                        savedStatus.lastCheckTime
                    )
                )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Internet Status",
                        fontSize = 28.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Контроль доступа по типу сети",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                TextButton(
                    onClick =
                        onOpenSettings
                ) {

                    Text("Настройки")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            NetworkCard(
                title =
                    "Мобильная сеть",

                subtitle =
                    "LTE / 5G",

                status =
                    mobileStatus
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            NetworkCard(
                title =
                    "Wi-Fi",

                subtitle =
                    "Беспроводная сеть",

                status =
                    wifiStatus
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Button(
                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    !checking,

                onClick = {

                    checking = true

                    mobileDetails =
                        "Выполняется проверка..."

                    wifiDetails =
                        "Выполняется проверка..."

                    scope.launch {
                        val checkSettings = settings
                        try {
                            runNetworkChecks(
                                check = { type ->
                                    checkNetwork(context, type, checkSettings)
                                },
                                onResult = { type, result ->
                                    when (type) {
                                        NetworkType.MOBILE -> {
                                            mobileStatus = result.status
                                            mobileDetails = result.details
                                            statusRepository.saveStatus(
                                                mobileStatus = result.status,
                                                mobileDetails = result.details,
                                                lastCheckTime = System.currentTimeMillis()
                                            )
                                        }
                                        NetworkType.WIFI -> {
                                            wifiStatus = result.status
                                            wifiDetails = result.details
                                            statusRepository.saveStatus(
                                                wifiStatus = result.status,
                                                wifiDetails = result.details
                                            )
                                        }
                                    }
                                }
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            val error = "Не удалось сохранить результат: " +
                                (e.message ?: e.javaClass.simpleName)
                            mobileDetails = error
                            wifiDetails = error
                        } finally {
                            checking = false
                        }
                    }
                }
            ) {

                if (checking) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
                            ),
                        strokeWidth =
                            2.dp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )

                    Text("Проверяем...")

                } else {

                    Text(
                        "Проверить сейчас"
                    )
                }
            }
            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    BackgroundCheckScheduler.runNow(
                        context.applicationContext
                    )
                }
            ) {
                Text("Тест фоновой проверки")
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )
            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(
                text =
                    "Последняя проверка: $lastCheck",

                fontSize =
                    14.sp,

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    "Таймаут: ${settings.timeoutMs} мс",

                fontSize =
                    13.sp,

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            Text(
                text = "Диагностика",
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            DiagnosticCard(
                title =
                    "Мобильная сеть",

                details =
                    mobileDetails
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            DiagnosticCard(
                title = "Wi-Fi",
                details = wifiDetails
            )
        }
    }
}

suspend fun checkNetwork(
    context: Context,
    networkType: NetworkType,
    settings: AppSettings
): NetworkCheckSummary =
    coroutineScope {

        val lease =
            NetworkProvider.requestNetwork(
                context =
                    context,

                networkType =
                    networkType
            )

        if (lease == null) {

            when (networkType) {

                NetworkType.WIFI -> {

                    val wifiManager =
                        context.applicationContext
                            .getSystemService(Context.WIFI_SERVICE)
                                as android.net.wifi.WifiManager

                    if (!wifiManager.isWifiEnabled) {

                        return@coroutineScope NetworkCheckSummary(
                            status = InternetStatus.WIFI_DISABLED,
                            details = "Wi-Fi на устройстве выключен."
                        )
                    }

                    return@coroutineScope NetworkCheckSummary(
                        status = InternetStatus.NETWORK_UNAVAILABLE,
                        details = "К Wi-Fi сети не подключён."
                    )
                }

                NetworkType.MOBILE -> {
                    return@coroutineScope NetworkCheckSummary(
                        status = InternetStatus.NETWORK_UNAVAILABLE,
                        details = "Мобильная сеть недоступна."
                    )
                }
            }
        }

        try {

            /*
             * 1. Сначала проверяем
             *    ПОЛНЫЙ ИНТЕРНЕТ.
             */

            val fullResult =
                checkGroup(
                    network =
                        lease.network,

                    urls =
                        settings.fullInternetUrls,

                    threshold =
                        settings.fullInternetThreshold,

                    timeoutMs =
                        settings.timeoutMs
                )

            if (fullResult.success) {

                return@coroutineScope NetworkCheckSummary(

                    status =
                        InternetStatus.FULL,

                    details =
                        buildGroupDetails(
                            title =
                                "Полный интернет",

                            result =
                                fullResult
                        ) +
                                "\n\n" +
                                "Белый список не проверялся: " +
                                "полный интернет уже подтверждён."
                )
            }

            /*
             * 2. Полного интернета нет.
             *    Проверяем белый список.
             */

            val whitelistResult =
                checkGroup(
                    network =
                        lease.network,

                    urls =
                        settings.whitelistUrls,

                    threshold =
                        settings.whitelistThreshold,

                    timeoutMs =
                        settings.timeoutMs
                )

            val finalStatus =

                if (
                    whitelistResult.success
                ) {

                    InternetStatus.WHITELIST

                } else {

                    InternetStatus.NO_INTERNET
                }

            NetworkCheckSummary(

                status =
                    finalStatus,

                details =

                    buildGroupDetails(
                        title =
                            "Полный интернет",

                        result =
                            fullResult
                    ) +

                            "\n\n" +

                            buildGroupDetails(
                                title =
                                    "Белый список",

                                result =
                                    whitelistResult
                            )
            )

        } finally {

            lease.release()
        }
    }

suspend fun checkGroup(
    network: Network,
    urls: List<String>,
    threshold: Int,
    timeoutMs: Int
): GroupCheckResult {

    if (urls.isEmpty()) {

        return GroupCheckResult(
            success = false,
            successCount = 0,
            requiredCount = threshold,
            checkedCount = 0,
            results = emptyList()
        )
    }

    val required =
        threshold.coerceIn(
            1,
            urls.size
        )

    val results =
        mutableListOf<CheckResult>()

    var successCount = 0

    for (
    (index, url) in
    urls.withIndex()
    ) {

        val result =
            NetworkChecker.checkUrl(
                network = network,
                url = url,
                timeoutMs = timeoutMs
            )

        results += result

        if (result.reachable) {
            successCount++
        }

        /*
         * Набрали нужное число
         * положительных ответов —
         * дальше сеть мучить не надо.
         */
        if (
            successCount >= required
        ) {
            break
        }

        /*
         * Если даже все оставшиеся
         * адреса ответят положительно,
         * но порог уже невозможно
         * набрать — заканчиваем.
         */
        val checked =
            index + 1

        val remaining =
            urls.size - checked

        if (
            successCount +
            remaining <
            required
        ) {
            break
        }
    }

    return GroupCheckResult(

        success =
            successCount >= required,

        successCount =
            successCount,

        requiredCount =
            required,

        checkedCount =
            results.size,

        results =
            results
    )
}

fun buildGroupDetails(
    title: String,
    result: GroupCheckResult
): String {

    if (
        result.results.isEmpty()
    ) {

        return "$title:\nСписок адресов пуст."
    }

    return buildString {

        appendLine("$title:")

        appendLine(
            "Успешно: " +
                    "${result.successCount} " +
                    "из ${result.checkedCount}"
        )

        appendLine(
            "Порог: " +
                    "${result.requiredCount}"
        )

        result.results.forEach {

            appendLine()

            append(
                formatResult(it)
            )
        }
    }
}

fun formatResult(
    result: CheckResult
): String {

    return if (
        result.reachable
    ) {

        "✓ ${result.url}\n" +
                "HTTP ${result.responseCode} • " +
                "${result.responseTimeMs} мс"

    } else {

        "✕ ${result.url}\n" +
                "${result.error ?: "Ошибка"} • " +
                "${result.responseTimeMs} мс"
    }
}

@Composable
fun NetworkCard(
    title: String,
    subtitle: String,
    status: InternetStatus
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    3.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(
                            color =
                                status.color,

                            shape =
                                CircleShape
                        )
            )

            Spacer(
                modifier =
                    Modifier.width(
                        18.dp
                    )
            )

            Column {

                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )

                Text(
                    text =
                        status.title,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        status.color
                )
            }
        }
    }
}

@Composable
fun DiagnosticCard(
    title: String,
    details: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                14.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(
                text = title,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    16.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    if (
                        details.isBlank()
                    ) {

                        "Проверка ещё не выполнялась."

                    } else {

                        details
                    },

                fontSize =
                    13.sp
            )
        }
    }
}
