// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class PrinterSessionTest {
    @Test fun preconnectStartsWithoutPrintingAndIsSharedAcrossALongPause() = runTest {
        val gate = CompletableDeferred<Unit>()
        val socket = FakeSocket(connectGate = gate)
        val fixture = Fixture(this, socket)
        fixture.session.select(firstJob)
        runCurrent()
        assertEquals(1, socket.connects)
        assertTrue(socket.labels.isEmpty())

        val first = async { fixture.print("first") }
        runCurrent()
        assertFalse(first.isCompleted)
        assertEquals(listOf("AA"), fixture.addresses)
        gate.complete(Unit)
        first.await()

        advanceTimeBy(10 * 60 * 1000)
        fixture.session.select(firstJob.copy())
        fixture.print("second")
        assertEquals(listOf("first", "second"), socket.labels)
        assertEquals(1, socket.connects)
        assertEquals(0, socket.closes)
        fixture.session.select(null)
        assertEquals(1, socket.closes)
    }

    @Test fun printWaitingForPreconnectGetsItsFailureWithoutReplaying() = runTest {
        val gate = CompletableDeferred<Unit>()
        val broken = FakeSocket(connectGate = gate)
        val next = FakeSocket()
        val fixture = Fixture(this, broken, next)
        fixture.session.select(firstJob)
        runCurrent()
        val first = async { runCatching { fixture.print("first") } }
        runCurrent()
        val failure = IOException("printer offline")
        gate.completeExceptionally(failure)

        val error = first.await().exceptionOrNull()
        assertTrue(error is IOException)
        assertEquals(failure.message, error?.message)
        assertEquals(listOf("AA"), fixture.addresses)
        assertEquals(1, broken.closes)
        assertTrue(broken.labels.isEmpty())
        fixture.print("second")
        assertEquals(listOf("second"), next.labels)
    }

    @Test fun failedUnusedPreconnectDoesNotLoopAndNextPrintCanConnect() = runTest {
        val broken = FakeSocket(connectGate = CompletableDeferred<Unit>().apply {
            completeExceptionally(IOException("printer offline"))
        })
        val next = FakeSocket()
        val fixture = Fixture(this, broken, next)
        fixture.session.select(firstJob)
        runCurrent()
        advanceTimeBy(60_000)
        assertEquals(listOf("AA"), fixture.addresses)
        fixture.print("label")
        assertEquals(listOf("label"), next.labels)
    }

    @Test fun changingJobsClosesAnUnusedConnectionEvenDuringConnect() = runTest {
        val old = FakeSocket(connectGate = CompletableDeferred())
        val next = FakeSocket()
        val fixture = Fixture(this, old, next)
        fixture.session.select(firstJob)
        runCurrent()
        fixture.session.select(AutoPrintPrinter(2, "BB"))
        assertEquals(1, old.closes)
        runCurrent()
        assertEquals(listOf("AA", "BB"), fixture.addresses)
        assertEquals(1, next.connects)
        assertTrue(old.labels.isEmpty())
    }

    @Test fun leavingBeforeAnAttemptStartsDoesNotOpenASocket() = runTest {
        val fixture = Fixture(this)
        fixture.session.select(firstJob)
        fixture.session.select(null)
        runCurrent()
        assertTrue(fixture.addresses.isEmpty())
    }

    @Test fun leavingDuringSocketCreationClosesItBeforeConnecting() = runTest {
        val socket = FakeSocket(closeFailure = true)
        lateinit var session: PrinterSession
        session = PrinterSession(backgroundScope, StandardTestDispatcher(testScheduler)) {
            session.select(null)
            socket
        }
        session.select(firstJob)
        runCurrent()
        assertEquals(0, socket.connects)
        assertEquals(1, socket.closes)
    }

    @Test fun leavingLetsTheCurrentWriteFinishAndQueuedWorkKeepsItsOriginalAddress() = runTest {
        val gate = CompletableDeferred<Unit>()
        val old = FakeSocket(writeGate = gate)
        val replacement = FakeSocket()
        val queued = FakeSocket()
        val fixture = Fixture(this, old, replacement, queued)
        fixture.session.select(firstJob)
        runCurrent()
        val print = async { fixture.print("in flight") }
        runCurrent()
        val queuedPrint = fixture.session.preparePrint("AA")
        fixture.session.select(AutoPrintPrinter(2, "BB"))
        runCurrent()
        assertEquals(listOf("in flight"), old.labels)
        assertEquals(0, old.closes)
        assertEquals(0, replacement.connects)

        gate.complete(Unit)
        print.await()
        runCurrent()
        queuedPrint("queued".toByteArray())
        assertEquals(listOf("AA", "BB", "AA"), fixture.addresses)
        assertEquals(listOf("queued"), queued.labels)
        assertTrue(replacement.labels.isEmpty())
        assertEquals(1, old.closes)
        assertEquals(1, queued.closes)
        assertEquals(0, replacement.closes)
    }

    @Test fun staleRequestCannotCreateARetainedSocketForAReopenedJob() = runTest {
        val old = FakeSocket()
        val failed = FakeSocket(connectGate = CompletableDeferred<Unit>().apply {
            completeExceptionally(IOException("printer offline"))
        })
        val oneShot = FakeSocket()
        val fixture = Fixture(this, old, failed, oneShot)
        fixture.session.select(firstJob)
        runCurrent()
        val queued = fixture.session.preparePrint("AA")
        fixture.session.select(null)
        fixture.session.select(firstJob)
        runCurrent()
        queued("queued".toByteArray())
        assertEquals(listOf("queued"), oneShot.labels)
        assertEquals(1, oneShot.closes)
    }

    @Test fun staleRequestCanBorrowAnAlreadyConnectedSocketForTheSamePrinter() = runTest {
        val old = FakeSocket()
        val replacement = FakeSocket()
        val fixture = Fixture(this, old, replacement)
        fixture.session.select(firstJob)
        runCurrent()
        val queued = fixture.session.preparePrint("AA")
        fixture.session.select(firstJob.copy(jobId = 2))
        runCurrent()
        queued("queued".toByteArray())
        assertEquals(listOf("queued"), replacement.labels)
        assertEquals(0, replacement.closes)
        assertEquals(1, old.closes)
    }

    @Test fun disconnectAndFailedWriteEachDiscardTheSocketWithoutReplaying() = runTest {
        val asleep = FakeSocket()
        val failedWrite = FakeSocket(writeFailure = true)
        val recovered = FakeSocket()
        val fixture = Fixture(this, asleep, failedWrite, recovered)
        fixture.session.select(firstJob)
        runCurrent()
        asleep.disconnected.complete(Unit)
        runCurrent()
        assertEquals(1, asleep.closes)
        assertEquals("write failed", runCatching { fixture.print("uncertain") }.exceptionOrNull()?.message)
        assertEquals(listOf("uncertain"), failedWrite.labels)
        assertEquals(1, failedWrite.closes)
        assertEquals(0, recovered.connects)

        fixture.print("next")
        assertEquals(listOf("next"), recovered.labels)
        recovered.disconnected.completeExceptionally(IOException("link lost"))
        runCurrent()
        assertEquals(1, recovered.closes)
    }

    @Test fun scanWarmsUpADisconnectedAutoPrintSocketOnlyOnce() = runTest {
        val disconnected = FakeSocket()
        val next = FakeSocket()
        val fixture = Fixture(this, disconnected, next)
        fixture.session.select(firstJob)
        runCurrent()
        disconnected.disconnected.complete(Unit)
        runCurrent()

        fixture.session.warmUp(firstJob)
        fixture.session.warmUp(firstJob)
        runCurrent()
        assertEquals(listOf("AA", "AA"), fixture.addresses)
        assertEquals(1, next.connects)
        assertTrue(next.labels.isEmpty())

        fixture.print("label")
        assertEquals(listOf("label"), next.labels)
    }

    @Test fun manualPrintsWithoutAutoPrintConnectAndCloseOneAtATime() = runTest {
        val gate = CompletableDeferred<Unit>()
        val first = FakeSocket(writeGate = gate)
        val second = FakeSocket()
        val fixture = Fixture(this, first, second)
        val one = async { fixture.print("first") }
        runCurrent()
        val two = async { fixture.print("second") }
        runCurrent()
        assertEquals(0, second.connects)
        gate.complete(Unit)
        one.await()
        two.await()
        assertEquals(listOf("first"), first.labels)
        assertEquals(listOf("second"), second.labels)
        assertEquals(1, first.closes)
        assertEquals(1, second.closes)
    }

    @Test fun printerTestFinishesAfterOneWriteAndReportsOnlyConnectionAndSendTime() = runTest {
        val connectGate = CompletableDeferred<Unit>()
        val writeGate = CompletableDeferred<Unit>()
        val socket = FakeSocket(connectGate = connectGate, writeGate = writeGate)
        val fixture = Fixture(this, socket)
        val timing = PrinterTestTiming { TimeUnit.MILLISECONDS.toNanos(testScheduler.currentTime) }
        val test = async { fixture.session.printTest("AA", "test label".toByteArray(), timing) }

        runCurrent()
        advanceTimeBy(3)
        connectGate.complete(Unit)
        runCurrent()
        advanceTimeBy(5)
        assertFalse(test.isCompleted)
        writeGate.complete(Unit)
        runCurrent()
        assertTrue(test.isCompleted)
        test.await()

        advanceTimeBy(1_000)
        val report = timing.report()
        assertEquals(listOf("test label"), socket.labels)
        assertEquals(1, socket.closes)
        assertEquals(3L, report.elapsedMs[PrinterTestStage.CONNECT])
        assertEquals(5L, report.elapsedMs[PrinterTestStage.SEND])
        assertEquals(8L, report.totalMs)
    }

    @Test fun printerChecksReuseTheSettingsConnectionWithoutAnotherConnect() = runTest {
        val gate = CompletableDeferred<Unit>()
        val socket = FakeSocket(connectGate = gate)
        val fixture = Fixture(this, socket)
        fixture.session.select(SettingsPrinter("AA"))
        runCurrent()
        assertEquals(1, socket.connects)
        assertTrue(socket.labels.isEmpty())
        gate.complete(Unit)
        runCurrent()

        repeat(2) {
            val timing = PrinterTestTiming { TimeUnit.MILLISECONDS.toNanos(testScheduler.currentTime) }
            fixture.session.printTest("AA", "test label".toByteArray(), timing)
            assertEquals(0L, timing.report().elapsedMs[PrinterTestStage.CONNECT])
        }

        assertEquals(listOf("AA"), fixture.addresses)
        assertEquals(listOf("test label", "test label"), socket.labels)
        assertEquals(0, socket.closes)
        fixture.session.select(null)
        assertEquals(1, socket.closes)
    }

    @Test fun printerTestQueueTimeoutLeavesTheOtherPrintRunning() = runTest {
        val writeGate = CompletableDeferred<Unit>()
        val socket = FakeSocket(writeGate = writeGate)
        val fixture = Fixture(this, socket)
        val normal = async { fixture.print("first") }
        runCurrent()
        val timing = PrinterTestTiming { TimeUnit.MILLISECONDS.toNanos(testScheduler.currentTime) }
        val test = async { runCatching { fixture.session.printTest("AA", "test label".toByteArray(), timing) } }

        advanceTimeBy(15_000)
        runCurrent()
        val failure = test.await().exceptionOrNull() as PrinterTestFailure
        val report = timing.report(failure.error)
        assertEquals(PrinterTestError.TIMEOUT, report.error)
        assertEquals(PrinterTestStage.QUEUE, report.failedStage)
        assertEquals(15_000L, report.totalMs)
        assertFalse(normal.isCompleted)
        assertEquals(listOf("AA"), fixture.addresses)
        writeGate.complete(Unit)
        normal.await()
    }

    private class Fixture(scope: TestScope, vararg sockets: FakeSocket) {
        val addresses = mutableListOf<String>()
        private val pendingSockets = sockets.toList().iterator()
        val session = PrinterSession(scope.backgroundScope, StandardTestDispatcher(scope.testScheduler)) {
            addresses += it
            pendingSockets.next()
        }

        suspend fun print(label: String) = session.preparePrint("AA")(label.toByteArray())
    }

    private class FakeSocket(
        private val connectGate: CompletableDeferred<Unit>? = null,
        private val writeGate: CompletableDeferred<Unit>? = null,
        private val writeFailure: Boolean = false,
        private val closeFailure: Boolean = false
    ) : PrinterConnection {
        val disconnected = CompletableDeferred<Unit>()
        val labels = mutableListOf<String>()
        var connects = 0
        var closes = 0

        override suspend fun connect() {
            connects++
            connectGate?.await()
        }

        override suspend fun write(payload: ByteArray) {
            labels += payload.decodeToString()
            writeGate?.await()
            if (writeFailure) throw IOException("write failed")
        }

        override suspend fun awaitDisconnect() = disconnected.await()

        override fun close() {
            closes++
            connectGate?.completeExceptionally(IOException("connection canceled"))
            disconnected.complete(Unit)
            if (closeFailure) throw IOException("close failed")
        }
    }

    private companion object {
        val firstJob = AutoPrintPrinter(1, "AA")
    }
}
