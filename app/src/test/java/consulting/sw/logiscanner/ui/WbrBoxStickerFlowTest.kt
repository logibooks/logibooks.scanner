// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.net.*
import consulting.sw.logiscanner.printer.*
import consulting.sw.logiscanner.repo.ScanJobMonitorRepository
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WbrBoxStickerFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Suppress("UNCHECKED_CAST")
    @Test fun boxPrintsFromDetailsAndFailuresRemainVisibleForRetry() {
        val bytes = "SIZE 58 mm,40 mm\r\nPRINT 1,1\r\n".toByteArray()
        val status = AtomicInteger(200)
        val body = AtomicReference("""{"template":"WBR_BOX","contentBase64":"${java.util.Base64.getEncoder().encodeToString(bytes)}","language":"TSPL","widthMm":58,"heightMm":40}""")
        val requests = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests += exchange.requestURI.toString() + "|" + exchange.requestHeaders.getFirst("Authorization")
            val content = body.get().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status.get(), content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        try {
            val application = RuntimeEnvironment.getApplication()
            shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            val repo = ScanJobMonitorRepository("http://127.0.0.1:${server.address.port}/api/", "box-token")
            val sent = ConcurrentLinkedQueue<ByteArray>()
            val gate = CompletableDeferred<Unit>()
            val client = object : StickerPrinterClient {
                override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()
                override suspend fun print(address: String, payload: ByteArray) { sent += payload; gate.await() }
            }
            lateinit var vm: MainViewModel
            lateinit var flow: MutableStateFlow<MainState>
            val box = ScanJobMonitorBox(boxId = 7, boxCode = "4542503776", boxSticker = "$1:1:4542503776:362877", printableTemplates = listOf("WBR_BOX"))
            val snapshot = ScanJobMonitorSnapshot(scanJobId = 1, registerType = RegisterTypes.WBR_N, companyId = 2,
                boxes = listOf(box), totalBoxes = 1)
            compose.runOnIdle {
                vm = ViewModelProvider(compose.activity)[MainViewModel::class.java]
                field(vm, "scanJobMonitorRepo", repo)
                field(vm, "stickerPrintService", KgtStickerPrintService(TscStickerRenderer(), client))
                val stateField = MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
                flow = stateField.get(vm) as MutableStateFlow<MainState>
                flow.value = flow.value.copy(isLoggedIn = true, printerBluetoothAddress = "AA:BB",
                    selectedScanJob = ScanJob(1, "Test", null, "15", "30", RegisterTypes.WBR_N),
                    monitorSnapshot = snapshot, monitorLoading = false)
            }
            assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,
                androidx.core.content.ContextCompat.checkSelfPermission(compose.activity, Manifest.permission.BLUETOOTH_CONNECT))
            assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,
                androidx.core.content.ContextCompat.checkSelfPermission(compose.activity, Manifest.permission.BLUETOOTH_SCAN))
            val printButton = compose.onNodeWithContentDescription("Печать стикера коробки")
            printButton.assertDoesNotExist()
            compose.onNodeWithText("Код стикера").assertDoesNotExist()
            compose.onNodeWithText("$1:1:4542503776:362877").assertDoesNotExist()
            compose.runOnIdle {
                flow.value = flow.value.copy(monitorSelectedScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOX, 7),
                    monitorDetailSnapshot = snapshot.copy(area = ScanJobMonitorAreas.BOX, boxes = emptyList(), box = box))
            }
            compose.onNodeWithText("Код стикера").assertExists()
            compose.onNodeWithText("$1:1:4542503776:362877").assertExists()
            printButton.performScrollTo().assertIsEnabled().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
            compose.waitUntil(10000) {
                shadowOf(android.os.Looper.getMainLooper()).idle()
                sent.size == 1 || vm.state.value.printerError != null
            }
            assertEquals("Requests=${requests.size}; error=${vm.state.value.printerError}; loading=${vm.state.value.printerLoading}", 1, sent.size)
            compose.runOnIdle { vm.printBoxSticker(7) }
            assertEquals(1, requests.size)
            assertTrue(vm.state.value.printerLoading)
            gate.complete(Unit)
            compose.waitUntil(10000) { shadowOf(android.os.Looper.getMainLooper()).idle(); !vm.state.value.printerLoading }
            assertArrayEquals(bytes, sent.single())
            assertEquals("/api/boxes/7/label?template=WBR_BOX|Bearer box-token", requests.single())
            status.set(409)
            body.set("""{"msg":"Box sticker unavailable","errors":{},"code":"Unavailable"}""")
            compose.onNodeWithContentDescription("Печать стикера коробки").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
            compose.waitUntil(10000) { shadowOf(android.os.Looper.getMainLooper()).idle(); !vm.state.value.printerLoading && vm.state.value.printerError != null }
            assertTrue(vm.state.value.printerError!!.contains("Box sticker unavailable"))
            compose.onNodeWithText(vm.state.value.printerError!!).assertExists()
            assertEquals(1, sent.size)
            // Missing/invalid error bodies preserve a useful HTTP error instead of hiding the failure.
            for (errorBody in listOf("", "{", "{\"msg\":\"\"}")) {
                body.set(errorBody)
                try { runBlocking { repo.loadBoxLabel(7) }; fail("Must reject HTTP failure") }
                catch (expected: java.io.IOException) { assertTrue(expected.message!!.contains("409")) }
            }
            status.set(200)
            body.set("""{"template":"WBR_BOX","contentBase64":"${java.util.Base64.getEncoder().encodeToString(bytes)}","language":"TSPL","widthMm":58,"heightMm":40}""")
            compose.onNodeWithContentDescription("Печать стикера коробки").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
            compose.waitUntil(10000) { shadowOf(android.os.Looper.getMainLooper()).idle(); sent.size == 2 && !vm.state.value.printerLoading }
            assertNull(vm.state.value.printerError)
            assertEquals(snapshot.boxes.single().boxSticker, vm.state.value.monitorDetailSnapshot!!.box!!.boxSticker)
        } finally { server.stop(0) }
    }

    private fun field(vm: MainViewModel, name: String, value: Any) {
        MainViewModel::class.java.getDeclaredField(name).apply { isAccessible = true }.set(vm, value)
    }
}
