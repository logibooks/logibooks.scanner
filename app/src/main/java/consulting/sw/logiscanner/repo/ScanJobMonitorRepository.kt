// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanJobMonitorObserveRequest
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class ScanJobMonitorScope(
    val area: Int,
    val boxId: Int? = null,
    val bucketIndex: Int? = null
)

class ScanJobMonitorRepository(
    baseUrl: String,
    private val token: String,
    onUnauthorized: (() -> Unit)? = null
) {
    private val api = NetworkModule.createApi(baseUrl, onUnauthorized)
    private val hubUrl = buildScanJobMonitorHubUrl(baseUrl)
    private val hubMutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var hubConnection: HubConnection? = null
    private var snapshotHandler: ((ScanJobMonitorSnapshot) -> Unit)? = null
    private var closedHandler: ((Int, Int) -> Unit)? = null
    private var connectionClosedHandler: ((Throwable?) -> Unit)? = null
    private var stopping = false
    private val closeStarted = AtomicBoolean(false)

    suspend fun loadSnapshot(scanJobId: Int, scope: ScanJobMonitorScope): ScanJobMonitorSnapshot {
        return api.getScanJobMonitor(
            bearer = "Bearer $token",
            id = scanJobId,
            area = scope.area,
            boxId = scope.boxId,
            bucketIndex = scope.bucketIndex
        )
    }

    suspend fun observe(
        scanJobId: Int,
        scope: ScanJobMonitorScope,
        onSnapshot: (ScanJobMonitorSnapshot) -> Unit,
        onClosed: (Int, Int) -> Unit,
        onConnectionClosed: (Throwable?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val connection = hubMutex.withLock {
                snapshotHandler = onSnapshot
                closedHandler = onClosed
                connectionClosedHandler = onConnectionClosed

                ensureConnection()
            }

            if (connection.connectionState != HubConnectionState.CONNECTED) {
                connection.start().blockingAwait()
            } else {
                // Clear any prior subscription before starting a new one on the same connection
                runCatching {
                    connection.invoke("ClearScanJobMonitor")
                        .timeout(HUB_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .blockingAwait()
                }.onFailure { exception ->
                    Log.w(TAG, "Failed to clear previous subscription before re-observing; proceeding with new ObserveScanJob anyway", exception)
                }
            }

            connection.invoke(
                "ObserveScanJob",
                ScanJobMonitorObserveRequest(
                    scanJobId = scanJobId,
                    area = scope.area,
                    boxId = scope.boxId,
                    bucketIndex = scope.bucketIndex
                )
            ).blockingAwait()
        }
    }

    suspend fun clearMonitor(): Boolean {
        return withContext(Dispatchers.IO) {
            hubMutex.withLock {
                val connection = hubConnection ?: return@withLock false
                if (connection.connectionState != HubConnectionState.CONNECTED) {
                    return@withLock false
                }

                connection.invoke("ClearScanJobMonitor").blockingAwait()
                true
            }
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            hubMutex.withLock {
                snapshotHandler = null
                closedHandler = null
                connectionClosedHandler = null

                val connection = hubConnection ?: return@withLock
                stopping = true
                try {
                    if (connection.connectionState == HubConnectionState.CONNECTED) {
                        runCatching {
                            connection.invoke("ClearScanJobMonitor")
                                .timeout(HUB_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .blockingAwait()
                        }.onFailure { exception ->
                            Log.w(TAG, "Failed to clear scan job monitor before stopping", exception)
                        }
                    }
                    runCatching {
                        connection.stop()
                            .timeout(HUB_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .blockingAwait()
                    }.onFailure { exception ->
                        Log.w(TAG, "Failed to stop scan job monitor hub", exception)
                    }
                    runCatching {
                        connection.close()
                    }.onFailure { exception ->
                        Log.w(TAG, "Failed to close scan job monitor hub", exception)
                    }
                } finally {
                    hubConnection = null
                    stopping = false
                }
            }
        }
    }

    fun closeInBackground() {
        if (!closeStarted.compareAndSet(false, true)) {
            return
        }
        cleanupScope.launch {
            runCatching {
                stop()
            }.onFailure { exception ->
                Log.w(TAG, "Failed to clean up scan job monitor repository", exception)
            }
            cleanupScope.cancel()
        }
    }

    private fun ensureConnection(): HubConnection {
        hubConnection?.let { return it }

        val connection = HubConnectionBuilder.create(hubUrl)
            .withAccessTokenProvider(Single.defer { Single.just(token) })
            .build()

        connection.on(
            "ScanJobMonitorSnapshot",
            { snapshot: ScanJobMonitorSnapshot -> snapshotHandler?.invoke(snapshot) },
            ScanJobMonitorSnapshot::class.java
        )
        connection.on(
            "ScanJobMonitorClosed",
            { scanJobId: Int, status: Int -> closedHandler?.invoke(scanJobId, status) },
            Int::class.javaObjectType,
            Int::class.javaObjectType
        )
        connection.onClosed { exception ->
            if (!stopping) {
                connectionClosedHandler?.invoke(exception)
            }
        }

        hubConnection = connection
        return connection
    }
}

private const val TAG = "ScanJobMonitorRepo"
private const val HUB_STOP_TIMEOUT_SECONDS = 5L

internal fun buildScanJobMonitorHubUrl(baseUrl: String): String {
    val trimmed = baseUrl.trimEnd('/')
    val root = if (trimmed.endsWith("/api", ignoreCase = true)) {
        trimmed.dropLast(4)
    } else {
        trimmed
    }
    return "$root/hubs/scan-jobs"
}
