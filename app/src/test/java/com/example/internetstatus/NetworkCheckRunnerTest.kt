package com.example.internetstatus

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkCheckRunnerTest {
    private val full = NetworkCheckSummary(InternetStatus.FULL, "Available")

    @Test
    fun publishesMobileWhileWifiIsStillRunning() = runBlocking {
        withTimeout(2000) {
            val wifiStarted = CompletableDeferred<Unit>()
            val finishWifi = CompletableDeferred<Unit>()
            val mobilePublished = CompletableDeferred<Unit>()
            val published = mutableListOf<NetworkType>()
            val job = launch {
                runNetworkChecks(
                    check = { type ->
                        if (type == NetworkType.WIFI) {
                            wifiStarted.complete(Unit)
                            finishWifi.await()
                        } else {
                            wifiStarted.await()
                        }
                        full
                    },
                    onResult = { type, _ ->
                        published += type
                        if (type == NetworkType.MOBILE) mobilePublished.complete(Unit)
                    }
                )
            }

            mobilePublished.await()
            assertEquals(listOf(NetworkType.MOBILE), published)
            assertFalse(job.isCompleted)
            finishWifi.complete(Unit)
            job.join()
            assertEquals(listOf(NetworkType.MOBILE, NetworkType.WIFI), published)
        }
    }

    @Test
    fun wifiFailureDoesNotDiscardMobileResult() = runBlocking {
        withTimeout(2000) {
            val wifiFailed = CompletableDeferred<Unit>()
            val results = mutableMapOf<NetworkType, NetworkCheckSummary>()
            runNetworkChecks(
                check = { type ->
                    if (type == NetworkType.WIFI) {
                        wifiFailed.complete(Unit)
                        error("Wi-Fi unavailable")
                    }
                    wifiFailed.await()
                    full
                },
                onResult = { type, result -> results[type] = result }
            )

            assertEquals(full, results[NetworkType.MOBILE])
            assertEquals(InternetStatus.NETWORK_UNAVAILABLE, results[NetworkType.WIFI]?.status)
            assertTrue(results.getValue(NetworkType.WIFI).details.contains("Wi-Fi unavailable"))
        }
    }

    @Test
    fun widgetCheckNeverRequestsWifi() = runBlocking {
        val requested = mutableListOf<NetworkType>()
        val published = mutableListOf<NetworkType>()
        runNetworkChecks(
            mobileOnly = true,
            check = { type -> requested += type; full },
            onResult = { type, _ -> published += type }
        )
        assertEquals(listOf(NetworkType.MOBILE), requested)
        assertEquals(listOf(NetworkType.MOBILE), published)
    }

    @Test
    fun cancellationStopsBothChecksWithoutPublishingFailure() = runBlocking {
        withTimeout(2000) {
            val mobileStarted = CompletableDeferred<Unit>()
            val wifiStarted = CompletableDeferred<Unit>()
            val stopped = mutableSetOf<NetworkType>()
            val published = mutableListOf<NetworkType>()
            val job = launch {
                runNetworkChecks(
                    check = { type ->
                        try {
                            when (type) {
                                NetworkType.MOBILE -> mobileStarted.complete(Unit)
                                NetworkType.WIFI -> wifiStarted.complete(Unit)
                            }
                            awaitCancellation()
                        } finally {
                            stopped += type
                        }
                    },
                    onResult = { type, _ -> published += type }
                )
            }
            mobileStarted.await()
            wifiStarted.await()
            job.cancelAndJoin()

            assertEquals(setOf(NetworkType.MOBILE, NetworkType.WIFI), stopped)
            assertTrue(published.isEmpty())
        }
    }
}
