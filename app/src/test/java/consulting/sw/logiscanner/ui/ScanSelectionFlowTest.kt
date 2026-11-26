// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.Manifest
import android.content.Intent
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.lifecycle.ViewModelProvider
import com.squareup.moshi.Moshi
import com.sun.net.httpserver.HttpServer
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.LookupScanCandidate
import consulting.sw.logiscanner.net.ParcelScanChoice
import consulting.sw.logiscanner.net.ParcelScanSelection
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanRequest
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.printer.BluetoothPrinterDevice
import consulting.sw.logiscanner.printer.KgtStickerPrintService
import consulting.sw.logiscanner.printer.StickerPrinterClient
import consulting.sw.logiscanner.printer.TscStickerRenderer
import consulting.sw.logiscanner.repo.ScanJobMonitorRepository
import consulting.sw.logiscanner.repo.ScanRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import org.robolectric.util.ReflectionHelpers
import java.io.IOException
import java.net.InetSocketAddress
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h800dp", shadows = [SettingsChecksTest.RecordingSpeech::class])
class ScanSelectionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private lateinit var core: ScanServer
    private lateinit var vm: MainViewModel
    private lateinit var state: MutableStateFlow<MainState>
    private lateinit var speech: SettingsChecksTest.RecordingSpeech

    @Before fun connectScanner() {
        core = ScanServer()
        compose.runOnIdle {
            vm = ViewModelProvider(compose.activity)[MainViewModel::class.java]
            state = ReflectionHelpers.getField(vm, "_state")
            ReflectionHelpers.setField(vm, "scanRepo", ScanRepository(core.url, "test"))
            state.value = state.value.copy(isLoggedIn = true, monitorAutoFollow = false)
            speech = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()) as SettingsChecksTest.RecordingSpeech
            ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("ru-RU"))
            speech.onInitListener.onInit(TextToSpeech.SUCCESS)
        }
    }

    @After fun closeScanner() {
        core.close()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun selectionBlocksScansKeepsOnlyCheckedParcelsAndShowsDiscardedCodes() {
        selectJob()
        val offerGate = core.reply(offer("Выбор для поиска"), hold = true)
        scan("SHARED", "QUEUED", "QUEUED")
        awaitRequests(1)
        offerGate.countDown()

        await { vm.state.value.parcelSelection?.discardedCodes == listOf("QUEUED") }
        val title = compose.onNodeWithText("Выберите товары")
        title.assertIsDisplayed()
        compose.onNodeWithText("QUEUED", substring = true).assertIsDisplayed()
        assertEquals("Выбор для поиска. Выберите товары", speech.utterances.single().text)

        compose.runOnIdle {
            compose.activity.sendBroadcast(
                Intent("nlscan.action.SCANNER_RESULT").putExtra("SCAN_BARCODE1", "UNEXPECTED-MT93")
            )
        }
        await { speech.utterances.size == 2 }
        compose.runOnIdle { state.value = state.value.copy(externalScannerEnabled = true) }
        title.performKeyInput {
            listOf(Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.Enter).forEach { pressKey(it) }
        }
        await { speech.utterances.size == 3 }
        assertEquals(listOf("Выберите товары", "Выберите товары"), speech.utterances.drop(1).map { it.text })
        assertEquals(listOf("SHARED"), core.requests.map(ScanRequest::code))

        compose.onNodeWithText("Третий", substring = true).performClick()

        val pending = core.reply(batch(
            result(items = listOf("P-2"), lookup = "confirmed", time = "2026-11-26T12:00:00Z"),
            result(items = listOf("P-3"), hasIssues = true, message = "Передайте на проверку", time = "")
        ), hold = true)
        compose.onNodeWithText("Подтвердить (2)").performClick()
        awaitRequests(2)
        assertEquals(ScanRequest(1, "SHARED", selectionId = offerId, selectedParcelIds = listOf(2, 3)), core.requests.last())
        compose.onNodeWithText("Подтвердить (2)").assertIsNotEnabled()
        compose.runOnIdle {
            vm.onScanned("DURING-SUBMIT")
            vm.confirmParcelSelection()
            vm.skipParcelSelection()
            vm.selectScanJob(null)
            vm.openSettings()
        }
        assertEquals(1, vm.state.value.selectedScanJob?.id)
        assertFalse(vm.state.value.settingsOpen)
        pending.countDown()

        await { vm.state.value.parcelSelection == null && vm.state.value.lastSelectedParcels.size == 2 }
        val finished = vm.state.value
        assertEquals(2, finished.lastParcelCount)
        assertEquals(listOf("P-2", "P-3"), finished.lastItemNumbers)
        assertEquals(ScanResultColor.ISSUE, finished.scanResultColor)
        assertEquals("Передайте на проверку", finished.lastSelectedParcels.last().message)
        assertTrue(finished.error.orEmpty().contains("QUEUED"))
        assertEquals(3, speech.utterances.size)
        assertEquals(listOf("SHARED", "SHARED"), core.requests.map(ScanRequest::code))

        core.reply(result(items = listOf("P-4")))
        scan("AFTER")
        await { vm.state.value.lastCode == "AFTER" }
        assertTrue(vm.state.value.lastSelectedParcels.isEmpty())
        shadowOf(Looper.getMainLooper()).idleFor(1600, TimeUnit.MILLISECONDS)
        assertEquals(ScanResultColor.NONE, vm.state.value.scanResultColor)
    }

    @Test fun selectionCanBeSkippedAndSubmissionErrorsRequireANewScan() {
        selectJob()
        core.reply(offer())
        scan("STALE")
        await { vm.state.value.parcelSelection != null }
        assertEquals("Выберите товары", speech.utterances.last().text)
        core.reject(409)
        compose.onNodeWithText("Подтвердить (3)").performClick()
        await { vm.state.value.parcelSelection?.stale == true }
        scan("IGNORED-BEFORE-CLOSE")
        assertEquals(1, speech.utterances.size)
        compose.onNodeWithText("Отсканируйте штрихкод снова.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
        await { vm.state.value.parcelSelection == null }

        core.reply(offer())
        scan("MALFORMED")
        await { vm.state.value.parcelSelection?.code == "MALFORMED" }
        core.reply(result())
        compose.onNodeWithText("Пропустить").performClick()
        await { vm.state.value.parcelSelection?.stale == true }
        compose.onNodeWithText("Закрыть").performClick()
        await { vm.state.value.parcelSelection == null }

        core.reply(offer())
        scan("SKIP")
        await { vm.state.value.parcelSelection?.code == "SKIP" }
        core.reply(batch())
        compose.onNodeWithText("Пропустить").performClick()
        await { vm.state.value.parcelSelection == null }
        assertFalse(vm.state.value.isBusy)
        assertTrue(vm.state.value.lastSelectedParcels.isEmpty())
        assertNull(vm.state.value.lastCode)
        assertEquals(listOf("STALE", "STALE", "MALFORMED", "MALFORMED", "SKIP", "SKIP"), core.requests.map(ScanRequest::code))
        assertEquals(emptyList<Int>(), core.requests.last().selectedParcelIds)
    }

    @Test fun rejectedJobDiscardsItsQueueBeforeMonitorDisconnectAndPreservesTheNextJobsQueue() {
        selectJob()
        val monitor = ScanJobMonitorRepository(core.url, "test")
        val disconnect = ReflectionHelpers.getField<Mutex>(monitor, "hubMutex")
        assertTrue(disconnect.tryLock())
        val failed = core.reject(400, hold = true)

        try {
            scan("REJECTED", "OLD-QUEUED")
            awaitRequests(1)
            compose.runOnIdle { ReflectionHelpers.setField(vm, "scanJobMonitorRepo", monitor) }
            failed.countDown()
            await { vm.state.value.selectedScanJob == null && !vm.state.value.isBusy }
            assertEquals(compose.activity.application.getString(R.string.scan_error_job_invalid), vm.state.value.error)
            scan("NO-JOB")
            compose.runOnIdle { ReflectionHelpers.setField(vm, "scanJobMonitorRepo", null) }
            selectJob(job.copy(id = 2))
            val next = core.reply(result(), hold = true)
            core.reply(result(items = listOf("NEW-PARCEL")))
            scan("NEW-FIRST", "NEW-QUEUED")
            awaitRequests(2)
            disconnect.unlock()
            next.countDown()
            await { vm.state.value.lastCode == "NEW-QUEUED" }
            assertEquals(listOf(1, 2, 2), core.requests.map(ScanRequest::id))
            assertEquals(listOf("REJECTED", "NEW-FIRST", "NEW-QUEUED"), core.requests.map(ScanRequest::code))
        } finally {
            if (disconnect.isLocked) disconnect.unlock()
        }
    }

    @Test fun serverErrorKeepsAcceptedScansAndStopOnlyRejectsNewInput() {
        selectJob()
        core.reject(503)
        scan("SERVER-ERROR")
        await { vm.state.value.scanResultColor == ScanResultColor.SERVER_ERROR && !vm.state.value.isBusy }
        assertEquals(compose.activity.application.getString(R.string.scan_error_server), vm.state.value.error)

        val inFlight = core.reply(result(), hold = true)
        val outstanding = LookupScanCandidate(2, "P-2", "017")
        val confirmed = LookupScanCandidate(3, "P-3", "017")
        core.reply(result().copy(
            count = 0, parcelCount = 0, boxCount = 1, scanSource = ScannedItemSources.BOX_STICKER,
            lookupOutcome = "inspectBox", lookupCandidates = listOf(outstanding), confirmedLookupCandidates = listOf(confirmed)
        ))
        scan("IN-FLIGHT", "BOX")
        awaitRequests(2)
        compose.runOnIdle {
            vm.stopScanning()
            vm.onScanned("AFTER-STOP")
        }
        inFlight.countDown()

        await { vm.state.value.lastCode == "BOX" && !vm.state.value.isBusy }
        assertFalse(vm.state.value.isScanning)
        assertEquals(ScanResultColor.OK, vm.state.value.scanResultColor)
        assertEquals(LookupBoxResult(listOf(outstanding), listOf(confirmed)), vm.state.value.lastLookupBox)
        assertEquals(listOf("SERVER-ERROR", "IN-FLIGHT", "BOX"), core.requests.map(ScanRequest::code))
    }

    @Test fun selectedLabelsPrintInOrderAndManualRecoveryClearsTheMatchingError() {
        selectJob(job.copy(registerType = RegisterTypes.WBR))
        val printer = RecordingPrinter()
        configurePrinter(printer)
        core.reply(offer())
        scan("PRINT")
        await { vm.state.value.parcelSelection != null }
        compose.onNodeWithText("Третий", substring = true).performClick()
        core.reply(batch(result(extId = "15001"), result(extId = "15002")))
        compose.onNodeWithText("Подтвердить (2)").performClick()

        await { printer.attempts.size == 2 && !vm.state.value.selectionPrinterLoading }
        assertNull(vm.state.value.parcelSelection)
        assertEquals(listOf("15001", "15002"), printer.attempts.toList())
        val failedLabel = SelectionPrintKey(job.id, 2)
        assertEquals(SelectionPrintWork.Kgt("15001"), vm.state.value.selectionPrintFailures.getValue(failedLabel).work)
        assertEquals(setOf(failedLabel), vm.state.value.selectionPrintFailures.keys)
        assertEquals(BulkyItemsModes.SILENT, core.requests.last().bulkyItemsMode)

        compose.runOnIdle { vm.printKgtSticker("15001") }
        await { printer.attempts.size == 3 && !vm.state.value.printerLoading }
        assertTrue(vm.state.value.selectionPrintFailures.isEmpty())
        assertEquals(R.string.printer_printed, vm.state.value.printerMessage)
    }

    @Test fun printFailuresSurviveJobChangesAndRetryClearsOnlyTheChosenParcel() {
        val firstJob = job.copy(name = "Первое", registerType = RegisterTypes.WBR)
        val secondJob = firstJob.copy(id = 2, name = "Второе")
        val heldLabel = CompletableDeferred<Unit>()
        val printer = RecordingPrinter { attempt ->
            if (attempt == 2) heldLabel.await()
            if (attempt <= 3) throw IOException("Printer is offline")
        }
        val firstFailure = SelectionPrintKey(firstJob.id, 2)
        val delayedFailure = SelectionPrintKey(firstJob.id, 3)
        val secondJobFailure = SelectionPrintKey(secondJob.id, 2)
        val firstLabel = "Задание Первое (#1), товар #2"
        val delayedLabel = "Задание Первое (#1), товар #3"
        val secondJobLabel = "Задание Второе (#2), товар #2"
        shadowOf(compose.activity.application).grantPermissions(
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN
        )

        try {
            selectJob(firstJob)
            configurePrinter(printer)
            core.reply(offer())
            scan("FIRST-BATCH")
            await { vm.state.value.parcelSelection != null }
            compose.onNodeWithText("Третий", substring = true).performClick()
            core.reply(batch(result(extId = "15001"), result(extId = "15002")))
            compose.onNodeWithText("Подтвердить (2)").performClick()
            await { printer.attempts.size == 2 && firstFailure in vm.state.value.selectionPrintFailures }

            compose.runOnIdle { vm.selectScanJob(null) }
            await { vm.state.value.selectedScanJob == null }
            compose.onNodeWithText(firstLabel).assertIsDisplayed()
            compose.onNodeWithText("Повторить печать").assertIsNotEnabled()

            selectJob(secondJob)
            configurePrinter(printer)
            heldLabel.complete(Unit)
            await { delayedFailure in vm.state.value.selectionPrintFailures && !vm.state.value.selectionPrinterLoading }
            compose.onNodeWithText(delayedLabel).assertIsDisplayed()

            core.reply(offer())
            scan("SECOND-BATCH")
            await { vm.state.value.parcelSelection != null }
            compose.onNodeWithText("Второй", substring = true).performClick()
            compose.onNodeWithText("Третий", substring = true).performClick()
            core.reply(batch(result(extId = "15001")))
            compose.onNodeWithText("Подтвердить (1)").performClick()
            await { secondJobFailure in vm.state.value.selectionPrintFailures && !vm.state.value.selectionPrinterLoading }

            compose.runOnIdle { vm.selectScanJob(null) }
            compose.onNodeWithText(secondJobLabel).assertIsDisplayed()
            assertEquals(setOf(firstFailure, delayedFailure, secondJobFailure), vm.state.value.selectionPrintFailures.keys)
            compose.onAllNodesWithText("Повторить печать")[0].performClick()
            await { firstFailure !in vm.state.value.selectionPrintFailures && !vm.state.value.selectionPrinterLoading }

            compose.onNodeWithText(firstLabel).assertDoesNotExist()
            compose.onNodeWithText(delayedLabel).assertIsDisplayed()
            compose.onNodeWithText(secondJobLabel).assertIsDisplayed()
            assertEquals(setOf(delayedFailure, secondJobFailure), vm.state.value.selectionPrintFailures.keys)
            assertEquals(listOf("15001", "15002", "15001", "15001"), printer.attempts.toList())
            assertEquals(4, core.requests.size)
        } finally {
            heldLabel.complete(Unit)
        }
    }

    private fun configurePrinter(printer: RecordingPrinter) = compose.runOnIdle {
        ReflectionHelpers.setField(vm, "stickerPrintService", KgtStickerPrintService(TscStickerRenderer(), printer))
        state.value = state.value.copy(
            printerBluetoothAddress = printerAddress,
            bulkyItemsMode = BulkyItemsModes.SILENT,
            printerAutoPrintEnabled = true
        )
    }

    private fun selectJob(selected: ScanJob = job) {
        compose.runOnIdle { vm.selectScanJob(selected) }
        await { vm.state.value.selectedScanJob?.id == selected.id }
        compose.runOnIdle { vm.startScanning() }
    }

    private fun scan(vararg codes: String) = compose.runOnIdle { codes.forEach(vm::onScanned) }

    private fun awaitRequests(count: Int) = await { core.requests.size >= count }

    private fun await(condition: () -> Boolean) = compose.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idle()
        condition()
    }

    private class RecordingPrinter(
        private val afterAttempt: suspend (Int) -> Unit = { if (it == 1) throw IOException("Printer is offline") }
    ) : StickerPrinterClient {
        val attempts = CopyOnWriteArrayList<String>()

        override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

        override suspend fun print(address: String, payload: ByteArray) {
            assertEquals(printerAddress, address)
            val code = listOf("15001", "15002").single { payload.decodeToString().contains(it) }
            attempts += code
            afterAttempt(attempts.size)
        }
    }

    private class ScanServer : AutoCloseable {
        val requests = CopyOnWriteArrayList<ScanRequest>()
        private val responses = LinkedBlockingQueue<Response>()
        private val gates = CopyOnWriteArrayList<CountDownLatch>()
        private val executor = Executors.newCachedThreadPool()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        private val moshi = Moshi.Builder().build()
        val url: String get() = "http://127.0.0.1:${server.address.port}/api/"

        init {
            server.executor = executor
            server.createContext("/api/ScanJobs/scan") { exchange ->
                exchange.use {
                    requests += moshi.adapter(ScanRequest::class.java).fromJson(exchange.requestBody.reader().readText())!!
                    val response = responses.poll(5, TimeUnit.SECONDS) ?: Response(500, "{}", CountDownLatch(0))
                    check(response.gate.await(10, TimeUnit.SECONDS))
                    val json = response.body.toByteArray()
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(response.status, json.size.toLong())
                    exchange.responseBody.write(json)
                }
            }
            server.start()
        }

        fun reply(result: ScanResultItem, hold: Boolean = false): CountDownLatch =
            enqueue(200, moshi.adapter(ScanResultItem::class.java).toJson(result), hold)

        fun reject(status: Int, hold: Boolean = false): CountDownLatch =
            enqueue(status, """{"msg":"Scan rejected"}""", hold)

        private fun enqueue(status: Int, body: String, hold: Boolean): CountDownLatch {
            val gate = CountDownLatch(if (hold) 1 else 0)
            gates += gate
            responses += Response(status, body, gate)
            return gate
        }

        override fun close() {
            gates.forEach(CountDownLatch::countDown)
            server.stop(0)
            executor.shutdownNow()
        }

        private data class Response(val status: Int, val body: String, val gate: CountDownLatch)
    }

    private companion object {
        const val offerId = "f00c98e9-09e5-4a94-88cb-d19af0e23299"
        const val printerAddress = "00:11:22:33:44:55"
        val job = ScanJob(1, "Scan test", null, "15", "30")

        fun result(
            items: List<String> = emptyList(),
            lookup: String? = null,
            hasIssues: Boolean = false,
            message: String? = null,
            time: String? = null,
            extId: String? = null
        ) = ScanResultItem(
            count = 1, parcelCount = 1, boxCount = 0, scanSource = ScannedItemSources.PARCEL_STICKER,
            itemNumbers = items, extData = message, lookupOutcome = lookup, hasIssues = hasIssues,
            extId = extId, scanTime = time
        )

        fun offer(message: String? = null) = result().copy(
            count = 0, parcelCount = 0, extData = message,
            parcelSelection = ParcelScanSelection(offerId, listOf(
                ParcelScanChoice(3, "Второй", "018", lookupState = "confirmed"),
                ParcelScanChoice(2, "Первый", "017", lookupState = "outstanding"),
                ParcelScanChoice(5, "Третий", lookupState = "notTarget")
            ))
        )

        fun batch(vararg results: ScanResultItem) = result().copy(selectionResults = results.toList())
    }
}
