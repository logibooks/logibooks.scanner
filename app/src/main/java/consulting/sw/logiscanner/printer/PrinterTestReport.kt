// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

internal enum class PrinterTestStage { QUEUE, PREPARE, CONNECT, SEND }

internal enum class PrinterTestError { TIMEOUT, PERMISSION, PRINTER_NOT_FOUND, CONNECTION, OTHER }

internal data class PrinterTestReport(
    val elapsedMs: Map<PrinterTestStage, Long>,
    val totalMs: Long,
    val failedStage: PrinterTestStage?,
    val error: PrinterTestError?
)

internal class PrinterTestFailure(val error: PrinterTestError) : Exception()

internal class PrinterTestTiming(
    private val nowNanos: () -> Long = System::nanoTime
) {
    private val startedAt = nowNanos()
    private val times = mutableMapOf<PrinterTestStage, Long>()
    private var stage: PrinterTestStage? = null
    private var lastStageEndedAt = startedAt

    suspend fun <T> measure(stage: PrinterTestStage, operation: suspend () -> T): T {
        this.stage = stage
        val start = nowNanos()
        return try {
            operation()
        } finally {
            lastStageEndedAt = nowNanos()
            times[stage] = times.getOrDefault(stage, 0) + (lastStageEndedAt - start)
        }
    }

    suspend fun <T> withQueueLock(mutex: Mutex, operation: suspend () -> T): T {
        val owner = Any()
        try {
            measure(PrinterTestStage.QUEUE) {
                val remaining = 15_000 - elapsedMs(times.getOrDefault(PrinterTestStage.QUEUE, 0))
                withTimeoutOrNull(remaining) { mutex.lock(owner) }
                    ?: throw PrinterTestFailure(PrinterTestError.TIMEOUT)
            }
            return operation()
        } finally {
            if (mutex.holdsLock(owner)) mutex.unlock(owner)
        }
    }

    fun report(error: PrinterTestError? = null): PrinterTestReport =
        PrinterTestReport(
            times.mapValues { elapsedMs(it.value) }, elapsedMs(lastStageEndedAt - startedAt),
            stage.takeIf { error != null }, error
        )

    private fun elapsedMs(nanos: Long) = TimeUnit.NANOSECONDS.toMillis(nanos)
}
