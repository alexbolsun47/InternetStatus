package com.example.internetstatus

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Runs both transports concurrently, publishing MOBILE before waiting for Wi-Fi. */
internal suspend fun runNetworkChecks(
    mobileOnly: Boolean = false,
    check: suspend (NetworkType) -> NetworkCheckSummary,
    onResult: suspend (NetworkType, NetworkCheckSummary) -> Unit
) = coroutineScope {
    suspend fun checkSafely(type: NetworkType): NetworkCheckSummary = try {
        check(type)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkCheckSummary(
            status = InternetStatus.NETWORK_UNAVAILABLE,
            details = "Ошибка проверки: ${e.message ?: e.javaClass.simpleName}"
        )
    }

    val mobile = async { checkSafely(NetworkType.MOBILE) }
    val wifi = if (mobileOnly) null else async { checkSafely(NetworkType.WIFI) }

    onResult(NetworkType.MOBILE, mobile.await())
    if (wifi != null) {
        onResult(NetworkType.WIFI, wifi.await())
    }
}
