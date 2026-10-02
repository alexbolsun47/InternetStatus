package com.example.internetstatus

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class CheckResult(
    val url: String,
    val reachable: Boolean,
    val responseCode: Int?,
    val responseTimeMs: Long?,
    val error: String? = null
)

object NetworkChecker {

    suspend fun checkUrl(
        network: Network,
        url: String,
        timeoutMs: Int = 3000
    ): CheckResult = withContext(Dispatchers.IO) {

        val startTime =
            System.currentTimeMillis()

        var connection: HttpURLConnection? = null

        try {

            val targetUrl = URL(url)

            /*
             * КЛЮЧЕВОЙ МОМЕНТ:
             *
             * соединение открывает не Android "как получится",
             * а конкретная сеть:
             *
             * Wi-Fi или Cellular.
             */
            connection =
                network.openConnection(targetUrl)
                        as HttpURLConnection

            connection.apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                useCaches = false

                setRequestProperty(
                    "User-Agent",
                    "InternetStatus/1.0"
                )
            }

            val responseCode =
                connection.responseCode

            val responseTime =
                System.currentTimeMillis() - startTime

            /*
             * Для определения доступности нам не важно,
             * получили мы 200, 301, 403 или 404.
             *
             * Главное — сервер реально ответил.
             */
            CheckResult(
                url = url,
                reachable = true,
                responseCode = responseCode,
                responseTimeMs = responseTime,
                error = null
            )

        } catch (e: Exception) {

            val responseTime =
                System.currentTimeMillis() - startTime

            CheckResult(
                url = url,
                reachable = false,
                responseCode = null,
                responseTimeMs = responseTime,
                error = e.javaClass.simpleName
            )

        } finally {

            connection?.disconnect()
        }
    }
}