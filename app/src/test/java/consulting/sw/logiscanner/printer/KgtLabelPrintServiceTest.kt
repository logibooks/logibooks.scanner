// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class KgtLabelPrintServiceTest {

    @Test
    fun printReturnsMissingPrinterWhenAddressIsBlank() = runTest {
        val service = KgtLabelPrintService(TscLabelRenderer(), RecordingClient())

        assertEquals(KgtLabelPrintResult.MissingPrinter, service.print(null, "15"))
    }

    @Test
    fun printRendersAndSendsPayload() = runTest {
        val client = RecordingClient()
        val service = KgtLabelPrintService(TscLabelRenderer(), client)

        val result = service.print("AA:BB", "15")

        assertEquals(KgtLabelPrintResult.Success, result)
        assertEquals("AA:BB", client.prints.single().address)
        assertTrue(client.prints.single().payload.contains("QRCODE"))
        assertTrue(client.prints.single().payload.contains("\"15\""))
    }

    @Test
    fun printFullRelabelingRendersPaddedParcelAndRegisterIds() = runTest {
        val client = RecordingClient()
        val service = KgtLabelPrintService(TscLabelRenderer(), client)

        val result = service.printFullRelabeling("AA:BB", parcelId = 123, registerId = 45)

        assertEquals(KgtLabelPrintResult.Success, result)
        assertEquals("AA:BB", client.prints.single().address)
        assertTrue(client.prints.single().payload.contains("\"000000123\""))
        assertTrue(client.prints.single().payload.contains("\"000045\""))
    }

    @Test
    fun printMapsKnownFailures() = runTest {
        assertEquals(
            KgtLabelPrintResult.PermissionMissing,
            KgtLabelPrintService(TscLabelRenderer(), FailingClient(PrinterPermissionMissingException()))
                .print("AA:BB", "15")
        )
        assertEquals(
            KgtLabelPrintResult.PrinterNotFound("AA:BB"),
            KgtLabelPrintService(TscLabelRenderer(), FailingClient(PrinterNotFoundException("AA:BB")))
                .print("AA:BB", "15")
        )
        assertTrue(
            KgtLabelPrintService(TscLabelRenderer(), RecordingClient())
                .print("AA:BB", "bad\"code") is KgtLabelPrintResult.InvalidLabel
        )
        assertTrue(
            KgtLabelPrintService(TscLabelRenderer(), FailingClient(IOException("boom")))
                .print("AA:BB", "15") is KgtLabelPrintResult.Failed
        )
    }

    @Test
    fun printSerializesConcurrentCalls() = runTest {
        val client = DelayingClient()
        val service = KgtLabelPrintService(TscLabelRenderer(), client)

        awaitAll(
            async { service.print("AA:BB", "15") },
            async { service.print("AA:BB", "16") }
        )

        assertEquals(1, client.maxActiveCalls)
        assertEquals(2, client.calls)
    }

    private data class PrintCall(
        val address: String,
        val payload: String
    )

    private class RecordingClient : LabelPrinterClient {
        val prints = mutableListOf<PrintCall>()

        override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

        override suspend fun print(address: String, payload: ByteArray) {
            prints += PrintCall(address, payload.toString(Charsets.UTF_8))
        }
    }

    private class FailingClient(private val exception: Exception) : LabelPrinterClient {
        override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

        override suspend fun print(address: String, payload: ByteArray) {
            throw exception
        }
    }

    private class DelayingClient : LabelPrinterClient {
        var calls = 0
        var maxActiveCalls = 0
        private var activeCalls = 0

        override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

        override suspend fun print(address: String, payload: ByteArray) {
            calls += 1
            activeCalls += 1
            maxActiveCalls = maxOf(maxActiveCalls, activeCalls)
            delay(25)
            activeCalls -= 1
        }
    }
}
