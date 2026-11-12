// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.sun.net.httpserver.HttpServer
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.StickerTemplates
import consulting.sw.logiscanner.printer.AutoPrintPrinter
import consulting.sw.logiscanner.printer.BluetoothPrinterClient
import consulting.sw.logiscanner.printer.PrinterNotFoundException
import consulting.sw.logiscanner.printer.PrinterUnavailableException
import consulting.sw.logiscanner.repo.ScanRepository
import consulting.sw.logiscanner.store.RelabelingSubmode
import consulting.sw.logiscanner.store.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothSocket
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.PipedOutputStream
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [AutoPrintBluetoothFlowTest.PrinterDevice::class, AutoPrintBluetoothFlowTest.PrinterSocket::class])
class AutoPrintBluetoothFlowTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Before fun pairPrinters() {
        opened.clear()
        reconnectGate.set(null)
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        @Suppress("DEPRECATION")
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val devices = setOf(adapter.getRemoteDevice(firstAddress), adapter.getRemoteDevice(secondAddress))
        shadowOf(adapter).setBondedDevices(devices)
    }

    @Test fun foregroundJobConnectsBeforeScanningAndReleasesAcrossOperatorTransitions() {
        val settings = SettingsStore(app)
        runBlocking {
            settings.setPrinterBluetoothAddress(firstAddress)
            settings.setPrinterAutoPrintEnabled(true)
            settings.setRelabelingSubmode(RelabelingSubmode.KGT)
        }
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        val vm = ViewModelProvider(activity.get())[MainViewModel::class.java]
        val state = stateOf(vm)
        try {
            waitFor("saved printer preferences") {
                state.value.printerBluetoothAddress == firstAddress && state.value.printerAutoPrintEnabled &&
                    state.value.relabelingSubmode == RelabelingSubmode.KGT
            }
            state.value = state.value.copy(isLoggedIn = true)
            assertNull(autoPrintPrinter(state.value))
            vm.selectScanJob(wbrJob)
            val first = connected(0)
            assertFalse(state.value.isScanning)
            assertEquals("", readLabel(first))

            vm.printKgtSticker("15")
            waitFor("manual print on the prepared socket") { state.value.printerMessage != null }
            assertTrue(readLabel(first).contains("15"))
            assertEquals(1, opened.size)

            vm.setPrinterAutoPrintEnabled(false)
            waitFor("socket release when auto-print is disabled") {
                !state.value.printerAutoPrintEnabled && !first.isConnected
            }
            vm.setPrinterAutoPrintEnabled(true)
            val enabled = connected(1)

            activity.pause().stop()
            waitFor("socket release in the background") { !enabled.isConnected }
            activity.restart().start().resume()
            val foreground = connected(2)

            vm.openSettings()
            waitFor("socket release when opening settings") { !foreground.isConnected }
            val settingsPrinter = connected(3)
            assertEquals(firstAddress, opened[3].first)
            assertEquals("", readLabel(settingsPrinter))
            repeat(2) {
                vm.requestPrinterTest(hasPermission = true)
                waitFor("printer check to send its label") { !state.value.printerTestRunning && state.value.printerTestReport != null }
                assertNull(state.value.printerTestReport?.error)
            }
            assertEquals(2, Regex("PRINT 1,1").findAll(readLabel(settingsPrinter)).count())
            assertEquals(4, opened.size)
            vm.setPrinterBluetoothAddress(secondAddress)
            waitFor("old Settings socket release when selecting a printer") { !settingsPrinter.isConnected }
            val changedInSettings = connected(4)
            assertEquals(secondAddress, opened[4].first)
            vm.closeSettings()
            waitFor("Settings socket release when closing settings") { !changedInSettings.isConnected }
            val changed = connected(5)
            assertEquals(secondAddress, opened[5].first)

            vm.selectScanJob(null)
            waitFor("socket release when leaving the job") { !changed.isConnected }
            vm.selectScanJob(wbrJob)
            val reopened = connected(6)
            vm.logout()
            waitFor("socket release and logout") { !reopened.isConnected && !state.value.isLoggedIn }
        } finally {
            activity.pause().stop().destroy()
        }
    }

    @Test fun eligibleWbrAndTajikistanScansPrintOnTheirPreparedSockets() {
        val settings = SettingsStore(app)
        runBlocking {
            settings.setPrinterBluetoothAddress(firstAddress)
            settings.setPrinterAutoPrintEnabled(true)
            settings.setRelabelingSubmode(RelabelingSubmode.KGT)
        }
        val body = AtomicReference("""{"count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"extId":"15"}""")
        val reconnectedBeforeResponse = AtomicBoolean(false)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            reconnectGate.get()?.let { gate ->
                reconnectedBeforeResponse.set(gate.await(3, TimeUnit.SECONDS))
            }
            val bytes = body.get().toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        val vm = ViewModelProvider(activity.get())[MainViewModel::class.java]
        val state = stateOf(vm)
        try {
            MainViewModel::class.java.getDeclaredField("scanRepo").apply { isAccessible = true }
                .set(vm, ScanRepository("http://127.0.0.1:${server.address.port}/api/", "test"))
            waitFor("saved printer preferences") {
                state.value.printerBluetoothAddress == firstAddress && state.value.printerAutoPrintEnabled &&
                    state.value.relabelingSubmode == RelabelingSubmode.KGT
            }
            state.value = state.value.copy(isLoggedIn = true)
            vm.selectScanJob(wbrJob)
            val wbr = connected(0)
            vm.toggleBulkyItemsMode()
            vm.startScanning()
            vm.onScanned("wbr-barcode")
            waitFor("WBR auto-print") { !state.value.isBusy && state.value.printerMessage != null }
            assertTrue(readLabel(wbr).contains("15"))
            assertEquals(1, opened.size)

            shadowOf(wbr).inputStreamFeeder.close()
            waitFor("WBR printer disconnect") { !wbr.isConnected }
            assertEquals(AutoPrintPrinter(wbrJob.id, firstAddress), autoPrintPrinter(state.value))
            reconnectGate.set(CountDownLatch(1))
            vm.onScanned("second-wbr-barcode")
            val reconnected = connected(1)
            waitFor("WBR auto-print after reconnect") {
                readLabel(reconnected).contains("15") && !state.value.isBusy && state.value.printerMessage != null
            }
            assertTrue("The new connection should be ready before the response", reconnectedBeforeResponse.get())
            reconnectGate.set(null)

            val tjJob = wbrJob.copy(id = 2, registerType = 0, stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT)
            vm.selectScanJob(tjJob)
            waitFor("WBR socket release when switching jobs") { !reconnected.isConnected }
            assertNull(autoPrintPrinter(state.value))
            vm.setRelabelingSubmode(RelabelingSubmode.FULL)
            assertNull(autoPrintPrinter(state.value))
            vm.toggleBulkyItemsMode()
            val tj = connected(2)
            assertFalse(state.value.isScanning)
            body.set("""
                {"count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"stickerTemplate":"TJ_EXPORT",
                  "exportSticker":{"orderNumber":"12345678901","dcBankID":"DCB_123","placesCount":1,
                    "dispatchDate":"2026-11-10","weightKg":1.0,"costRub":200,"senderName":"Sender",
                    "senderAddress":"Sender address","recipientAddress":"Recipient address","recipientPhone":"123",
                    "items":[{"productName":"Goods","quantity":1}]}}
            """.trimIndent())
            vm.startScanning()
            vm.onScanned("tj-barcode")
            waitFor("TJ auto-print") { !state.value.isBusy && state.value.printerMessage != null }
            assertTrue(readLabel(tj).contains("12345678901"))
            assertEquals(3, opened.size)
        } finally {
            reconnectGate.getAndSet(null)?.countDown()
            activity.pause().stop().destroy()
            server.stop(0)
        }
    }

    @Test fun clientDropsAConnectionAfterFlushFailsAndDoesNotReplayBytes() = runBlocking {
        val client = BluetoothPrinterClient(app, this)
        val payload = "PRINT 1,1\r\n".toByteArray()
        try {
            client.selectPrinter(AutoPrintPrinter(1, firstAddress))
            val socket = connected(0)
            val writes = mutableListOf<ByteArray>()
            shadowOf(socket).setOutputStream(object : PipedOutputStream() {
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    writes += bytes.copyOfRange(offset, offset + length)
                }

                override fun flush() = throw IOException("flush failed")
            })
            val failure = runCatching { client.print(firstAddress, payload) }.exceptionOrNull()
            assertTrue(failure is PrinterUnavailableException)
            assertEquals("flush failed", failure!!.cause!!.message)
            assertArrayEquals(payload, writes.single())
            assertFalse(socket.isConnected)
            assertEquals(1, opened.size)

            client.print(firstAddress, payload)
            val next = connected(1)
            assertEquals(payload.decodeToString(), readLabel(next))
            shadowOf(next).inputStreamFeeder.write(byteArrayOf(0x01))
            shadowOf(next).inputStreamFeeder.close()
            waitFor("socket release after remote disconnect") { !next.isConnected }
            assertTrue(runCatching { client.print("01:02:03:04:05:06", payload) }.exceptionOrNull() is PrinterNotFoundException)
        } finally {
            client.selectPrinter(null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun stateOf(vm: MainViewModel): MutableStateFlow<MainState> =
        MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }.get(vm) as MutableStateFlow<MainState>

    private fun connected(index: Int): BluetoothSocket {
        waitFor("connection ${index + 1}") { opened.getOrNull(index)?.second?.isConnected == true }
        return opened[index].second
    }

    private fun readLabel(socket: BluetoothSocket): String =
        Shadow.extract<PrinterSocket>(socket).payload.toByteArray().decodeToString()

    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        } while (System.nanoTime() < deadline)
        val sockets = opened.joinToString { (address, socket) -> "$address connected=${socket.isConnected}" }
        assertTrue("Timed out waiting for $description (sockets: $sockets)", condition())
    }

    @Implements(BluetoothDevice::class)
    class PrinterDevice : ShadowBluetoothDevice() {
        @RealObject private lateinit var device: BluetoothDevice

        @Implementation
        override fun createRfcommSocketToServiceRecord(uuid: UUID): BluetoothSocket {
            val socket = Shadow.newInstanceOf(BluetoothSocket::class.java)
            val shadow = Shadow.extract<PrinterSocket>(socket)
            shadow.setOutputStream(object : PipedOutputStream() {
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    shadow.payload.write(bytes, offset, length)
                }
            })
            opened += device.address to socket
            return socket
        }
    }

    @Implements(BluetoothSocket::class)
    class PrinterSocket : ShadowBluetoothSocket() {
        val payload = ByteArrayOutputStream()

        @Implementation
        override fun connect() {
            super.connect()
            reconnectGate.get()?.countDown()
        }

        @Implementation
        override fun close() {
            super.close()
            inputStreamFeeder.close()
        }
    }

    private companion object {
        const val firstAddress = "00:11:22:33:44:55"
        const val secondAddress = "AA:BB:CC:DD:EE:FF"
        val opened = CopyOnWriteArrayList<Pair<String, BluetoothSocket>>()
        val reconnectGate = AtomicReference<CountDownLatch?>()
        val wbrJob = ScanJob(1, "WBR", null, "15", "30", RegisterTypes.WBR)
    }
}
