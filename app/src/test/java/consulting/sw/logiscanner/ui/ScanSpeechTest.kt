// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModelProvider
import com.sun.net.httpserver.HttpServer
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.repo.ScanJobMonitorRepository
import consulting.sw.logiscanner.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import org.robolectric.util.ReflectionHelpers
import java.net.InetSocketAddress
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [SettingsChecksTest.RecordingSpeech::class])
class ScanSpeechTest {
    @Test
    fun queuesRepeatsAndClearsSpeechWhenLeavingTheJob() {
        val requests = AtomicInteger()
        val lateRequest = CountDownLatch(1)
        val finishLateRequest = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val phrase = when (requests.incrementAndGet()) {
                3 -> {
                    lateRequest.countDown()
                    check(finishLateRequest.await(10, TimeUnit.SECONDS))
                    "Поздний ответ"
                }
                4 -> "Новая задача"
                else -> "Посылка найдена"
            }
            val response = """
                {"count":0,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],
                 "lookupOutcome":"alreadyConfirmed","extData":"$phrase"}
            """.trimIndent().toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}/api/"
            val job = ScanJob(1, "Lookup", null, "InProgress", "Both")
            val vm = ViewModelProvider(activity.get())[MainViewModel::class.java]
            val state = ReflectionHelpers.getField<MutableStateFlow<MainState>>(vm, "_state")
            val speech = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()) as SettingsChecksTest.RecordingSpeech
            ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("ru-RU"))
            speech.onInitListener.onInit(TextToSpeech.SUCCESS)
            ReflectionHelpers.setField(vm, "scanRepo", ScanRepository(baseUrl, "test"))
            state.value = state.value.copy(isLoggedIn = true)

            vm.selectScanJob(job)
            waitFor("first job to be selected") { state.value.selectedScanJob?.id == job.id }
            vm.startScanning()
            vm.onScanned("REPEAT-SCAN")
            waitFor("first phrase") { speech.utterances.size == 1 }
            vm.onScanned("REPEAT-SCAN")
            waitFor("repeat phrase") { speech.utterances.size == 2 }
            vm.onScanned("LATE-SCAN")
            waitFor("late request to reach the server") { lateRequest.count == 0L }

            val stopsBeforeSwitch = speech.stopCalls
            vm.selectScanJob(job.copy(id = 2))
            waitFor("second job to be selected") { state.value.selectedScanJob?.id == 2 }
            assertEquals(stopsBeforeSwitch + 1, speech.stopCalls)
            finishLateRequest.countDown()
            vm.startScanning()
            vm.onScanned("NEXT-JOB")
            waitFor("second job's phrase") { speech.utterances.lastOrNull()?.text == "Новая задача" }
            assertEquals(listOf("Посылка найдена", "Посылка найдена", "Новая задача"), speech.utterances.map { it.text })
            assertTrue(speech.utterances.all { it.queueMode == TextToSpeech.QUEUE_ADD })

            val monitor = ScanJobMonitorRepository(baseUrl, "test")
            ReflectionHelpers.setField(vm, "scanJobMonitorRepo", monitor)
            val disconnect = ReflectionHelpers.getField<Mutex>(monitor, "hubMutex")
            assertTrue(disconnect.tryLock())
            try {
                vm.selectScanJob(null)
                waitFor("job to be cleared during disconnect") { state.value.selectedScanJob == null }
                vm.startScanning()
                vm.onScanned("DURING-DISCONNECT")
                assertNull(state.value.selectedScanJob)
                assertFalse(state.value.isScanning)
                assertEquals(4, requests.get())
            } finally {
                disconnect.unlock()
            }
        } finally {
            finishLateRequest.countDown()
            activity.pause().stop().destroy()
            server.stop(0)
        }
    }

    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        } while (System.nanoTime() < deadline)
        error("Timed out waiting for $description")
    }
}
