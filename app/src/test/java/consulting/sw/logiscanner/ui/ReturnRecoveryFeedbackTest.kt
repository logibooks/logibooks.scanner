// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelProvider
import com.sun.net.httpserver.HttpServer
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ReturnRecoveryProgress
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class ReturnRecoveryFeedbackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Suppress("UNCHECKED_CAST")
    @Test
    fun scannerDistinguishesBoxAdviceRecoveryAndErrorsAndDisplaysCoreCounts() {
        val response = AtomicReference(200 to "{}")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val (status, body) = response.get()
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.requestBody.close()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val job = ScanJob(1, "Return recovery", null, "15", "30", RegisterTypes.WBR_N)
            val snapshot = ScanJobMonitorSnapshot(scanJobId = job.id, totalParcels = 20)
            lateinit var vm: MainViewModel
            lateinit var state: MutableStateFlow<MainState>
            compose.runOnIdle {
                vm = ViewModelProvider(compose.activity)[MainViewModel::class.java]
                MainViewModel::class.java.getDeclaredField("scanRepo").apply { isAccessible = true }
                    .set(vm, ScanRepository("http://127.0.0.1:${server.address.port}/api/", "test-token"))
                val stateField = MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
                state = stateField.get(vm) as MutableStateFlow<MainState>
                state.value = state.value.copy(
                    isLoggedIn = true, selectedScanJob = job, isScanning = true,
                    monitorSnapshot = snapshot, monitorAutoFollow = false
                )
            }

            val remainingLabel = compose.activity.getString(R.string.monitor_recovery_remaining)
            val recoveredLabel = compose.activity.getString(R.string.monitor_recovery_recovered)
            val confirmed = compose.activity.getString(R.string.monitor_recovery_confirmed)
            val instruction = "Откройте коробку и проверьте посылки"
            compose.onNodeWithText(remainingLabel).assertDoesNotExist()
            compose.onNodeWithText(recoveredLabel).assertDoesNotExist()
            compose.runOnIdle {
                state.value = state.value.copy(monitorSnapshot = snapshot.copy(returnRecovery = ReturnRecoveryProgress(7, 11)))
            }
            compose.onNodeWithText(remainingLabel).assertExists()
            compose.onNodeWithText("7").assertExists()
            compose.onNodeWithText(recoveredLabel).assertExists()
            compose.onNodeWithText("11").assertExists()

            fun submit(code: String, status: Int, body: String) {
                response.set(status to body)
                compose.runOnIdle { vm.onScanned(code) }
                compose.waitUntil(10000) {
                    shadowOf(Looper.getMainLooper()).idle()
                    !vm.state.value.isBusy && if (status == 400) {
                        vm.state.value.selectedScanJob == null
                    } else {
                        vm.state.value.lastCode == code
                    }
                }
            }

            submit("STICKER-BOX", 200, """{
                "count":0,"parcelCount":0,"boxCount":1,"scanSource":20,"itemNumbers":["ORIGINAL-BOX"],"extData":null,
                "recoveryBoxAdvice":{"boxNumber":"ORIGINAL-BOX","candidateParcelNumbers":["P-001","P-002"],"instruction":"$instruction"}
            }""")
            compose.onNodeWithText("ORIGINAL-BOX").assertExists()
            compose.onNodeWithText(instruction).assertExists()
            compose.onNodeWithText("P-001").assertExists()
            compose.onNodeWithText("P-002").assertExists()
            compose.onNodeWithText(confirmed).assertDoesNotExist()
            compose.onNodeWithText("7").assertExists()
            compose.onNodeWithText("11").assertExists()

            submit("STICKER-P3", 200, """{
                "count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":["P-003"],"extData":null,"recoveryConfirmed":true
            }""")
            compose.onNodeWithText("P-003").assertExists()
            compose.onNodeWithText(confirmed).assertExists()
            compose.onNodeWithText(instruction).assertDoesNotExist()
            compose.runOnIdle {
                state.value = state.value.copy(monitorSnapshot = snapshot.copy(returnRecovery = ReturnRecoveryProgress(6, 12)))
            }
            compose.onNodeWithText("6").assertExists()
            compose.onNodeWithText("12").assertExists()

            submit("AMBIGUOUS", 409, """{"msg":"Код соответствует нескольким посылкам","errors":{},"code":"AmbiguousReturnParcelSticker"}""")
            assertEquals(
                compose.activity.application.getString(R.string.scan_error_ambiguous_return_parcel, "AMBIGUOUS"),
                vm.state.value.error
            )
            compose.onNodeWithText(confirmed).assertDoesNotExist()
            assertNull(vm.state.value.lastRecoveryBoxAdvice)
            assertFalse(vm.state.value.lastRecoveryConfirmed)

            submit("CONFLICT", 409, """{"msg":"Other conflict","errors":{},"code":"Other"}""")
            assertEquals(compose.activity.application.getString(R.string.scan_error_server), vm.state.value.error)
            submit("MALFORMED", 409, "{")
            assertEquals(compose.activity.application.getString(R.string.scan_error_server), vm.state.value.error)

            submit("CLOSED", 400, """{"msg":"Задание завершено","errors":{}}""")
            assertNull(vm.state.value.lastRecoveryBoxAdvice)
            assertFalse(vm.state.value.lastRecoveryConfirmed)
        } finally {
            server.stop(0)
        }
    }
}
