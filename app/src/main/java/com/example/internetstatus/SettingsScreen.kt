package com.example.internetstatus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBack: () -> Unit
) {

    val savedSettings by repository.settings.collectAsState(
        initial = AppSettings()
    )

    var fullUrlsText by remember(savedSettings) {
        mutableStateOf(
            savedSettings.fullInternetUrls.joinToString("\n")
        )
    }

    var whitelistUrlsText by remember(savedSettings) {
        mutableStateOf(
            savedSettings.whitelistUrls.joinToString("\n")
        )
    }

    var timeoutText by remember(savedSettings) {
        mutableStateOf(
            savedSettings.timeoutMs.toString()
        )
    }

    var fullThresholdText by remember(savedSettings) {
        mutableStateOf(
            savedSettings.fullInternetThreshold.toString()
        )
    }

    var whitelistThresholdText by remember(savedSettings) {
        mutableStateOf(
            savedSettings.whitelistThreshold.toString()
        )
    }

    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp)
            ) {

                Text(
                    text = "Настройки",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Адреса указываются по одному на строку",
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "Полный интернет",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = fullUrlsText,
                    onValueChange = {
                        fullUrlsText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("HTTPS-адреса")
                    },
                    placeholder = {
                        Text(
                            "https://www.google.com\n" +
                                    "https://www.cloudflare.com"
                        )
                    },
                    minLines = 3
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = fullThresholdText,
                    onValueChange = {
                        fullThresholdText =
                            it.filter(Char::isDigit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Минимум успешных адресов")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "Белый список",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = whitelistUrlsText,
                    onValueChange = {
                        whitelistUrlsText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("HTTPS-адреса")
                    },
                    placeholder = {
                        Text(
                            "https://yandex.ru\n" +
                                    "https://example.ru"
                        )
                    },
                    minLines = 3
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = whitelistThresholdText,
                    onValueChange = {
                        whitelistThresholdText =
                            it.filter(Char::isDigit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Минимум успешных адресов")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "Проверка",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = timeoutText,
                    onValueChange = {
                        timeoutText =
                            it.filter(Char::isDigit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Таймаут, мс")
                    },
                    supportingText = {
                        Text(
                            "Рекомендуемое значение: $DEFAULT_TIMEOUT_MS мс"
                        )
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),
                    singleLine = true
                )

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                ) {
                    Text("Отмена")
                }

                Button(
                    modifier = Modifier.weight(1f),

                    onClick = {

                        val fullUrls =
                            parseUrls(fullUrlsText)

                        val whitelistUrls =
                            parseUrls(
                                whitelistUrlsText
                            )

                        val timeout =
                            timeoutText
                                .toIntOrNull()
                                ?.coerceIn(
                                    500,
                                    15000
                                )
                                ?: DEFAULT_TIMEOUT_MS

                        val fullThreshold =
                            fullThresholdText
                                .toIntOrNull()
                                ?.coerceAtLeast(1)
                                ?: 1

                        val whitelistThreshold =
                            whitelistThresholdText
                                .toIntOrNull()
                                ?.coerceAtLeast(1)
                                ?: 1

                        val settings =
                            AppSettings(

                                fullInternetUrls =
                                    fullUrls,

                                whitelistUrls =
                                    whitelistUrls,

                                timeoutMs =
                                    timeout,

                                fullInternetThreshold =
                                    fullThreshold.coerceAtMost(
                                        maxOf(
                                            1,
                                            fullUrls.size
                                        )
                                    ),

                                whitelistThreshold =
                                    whitelistThreshold.coerceAtMost(
                                        maxOf(
                                            1,
                                            whitelistUrls.size
                                        )
                                    )
                            )

                        scope.launch {

                            repository.saveSettings(
                                settings
                            )

                            onBack()
                        }
                    }
                ) {

                    Text("Сохранить")
                }

            }
        }
    }
}

private fun parseUrls(
    text: String
): List<String> {

    return text
        .lines()
        .map {
            it.trim()
        }
        .filter {
            it.isNotBlank()
        }
        .map {

            /*
             * Если пользователь написал просто:
             *
             * google.com
             *
             * автоматически превращаем это в:
             *
             * https://google.com
             */

            if (
                it.startsWith("http://") ||
                it.startsWith("https://")
            ) {
                it
            } else {
                "https://$it"
            }
        }
        .distinct()
}
