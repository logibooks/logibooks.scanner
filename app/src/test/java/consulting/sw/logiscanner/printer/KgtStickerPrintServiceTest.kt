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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KgtStickerPrintServiceTest {

    @Test
    fun printReturnsMissingPrinterWhenAddressIsBlank() = runTest {
        val service = KgtStickerPrintService(TscStickerRenderer(), RecordingClient())

        assertEquals(KgtStickerPrintResult.MissingPrinter, service.print(null, "15"))
    }

    @Test
    fun printRendersAndSendsPayload() = runTest {
        val client = RecordingClient()
        val service = KgtStickerPrintService(TscStickerRenderer(), client)

        val result = service.print("AA:BB", "15")

        assertEquals(KgtStickerPrintResult.Success, result)
        assertEquals("AA:BB", client.prints.single().address)
        assertTrue(client.prints.single().payload.contains("QRCODE"))
        assertTrue(client.prints.single().payload.contains("\"15\""))
    }

    @Test
    fun printTajikistanExportStickerSendsVectorAndBarcodePayload() = runTest {
        val client = RecordingClient()
        val service = KgtStickerPrintService(TscStickerRenderer(), client)

        val result = service.printTajikistanExportSticker("AA:BB", printableSticker())

        assertEquals(KgtStickerPrintResult.Success, result)
        assertTrue(!client.prints.single().payload.contains("BITMAP"))
        assertTrue(client.prints.single().payload.contains("CODEPAGE 1251"))
        assertTrue(client.prints.single().payload.contains("TEXT "))
        assertTrue(client.prints.single().payload.contains("BARCODE"))
        assertTrue(client.prints.single().payload.contains("\"40856360164\""))
    }

    @Test
    fun printParcelStickerSendsWbrNPayload() = runTest {
        val client = RecordingClient()
        val service = KgtStickerPrintService(TscStickerRenderer(), client)

        val result = service.printParcelSticker(
            "AA:BB",
            ParcelSticker.WbrN("STICKER-1", "39639934424")
        )

        assertEquals(KgtStickerPrintResult.Success, result)
        assertTrue(!client.prints.single().payload.contains("BITMAP"))
        assertTrue(client.prints.single().payload.contains("CODEPAGE 1251"))
        assertTrue(client.prints.single().payload.contains("QRCODE"))
        assertTrue(client.prints.single().payload.contains("\"39639934424\""))
    }

    @Test
    fun printParcelStickerRejectsOzonWithoutCallingPrinter() = runTest {
        val client = RecordingClient()
        val service = KgtStickerPrintService(TscStickerRenderer(), client)

        val result = service.printParcelSticker(
            "AA:BB",
            ParcelSticker.Ozon("POST-1", "OZON-BARCODE", "Ташкент")
        )

        assertTrue(result is KgtStickerPrintResult.InvalidSticker)
        assertTrue(client.prints.isEmpty())
    }

    @Test
    fun printMapsKnownFailures() = runTest {
        assertEquals(
            KgtStickerPrintResult.PermissionMissing,
            KgtStickerPrintService(TscStickerRenderer(), FailingClient(PrinterPermissionMissingException()))
                .print("AA:BB", "15")
        )
        assertEquals(
            KgtStickerPrintResult.PrinterNotFound("AA:BB"),
            KgtStickerPrintService(TscStickerRenderer(), FailingClient(PrinterNotFoundException("AA:BB")))
                .print("AA:BB", "15")
        )
        assertTrue(
            KgtStickerPrintService(TscStickerRenderer(), RecordingClient())
                .print("AA:BB", "bad\"code") is KgtStickerPrintResult.InvalidSticker
        )
        assertTrue(
            KgtStickerPrintService(TscStickerRenderer(), FailingClient(IOException("boom")))
                .print("AA:BB", "15") is KgtStickerPrintResult.Failed
        )
    }

    @Test
    fun printSerializesConcurrentCalls() = runTest {
        val client = DelayingClient()
        val service = KgtStickerPrintService(TscStickerRenderer(), client)

        awaitAll(
            async { service.print("AA:BB", "15") },
            async { service.print("AA:BB", "16") }
        )

        assertEquals(1, client.maxActiveCalls)
        assertEquals(2, client.calls)
    }

    @Test
    fun printSerializesKgtAndTajikistanCallsThroughOneMutex() = runTest {
        val client = DelayingClient()
        val service = KgtStickerPrintService(TscStickerRenderer(), client)

        awaitAll(
            async { service.print("AA:BB", "15") },
            async { service.printTajikistanExportSticker("AA:BB", printableSticker()) }
        )

        assertEquals(1, client.maxActiveCalls)
        assertEquals(2, client.calls)
    }

    private data class PrintCall(
        val address: String,
        val payload: String
    )

    private class RecordingClient : StickerPrinterClient {
        val prints = mutableListOf<PrintCall>()

        override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

        override suspend fun print(address: String, payload: ByteArray) {
            prints += PrintCall(address, payload.toString(Charsets.UTF_8))
        }
    }

    private class FailingClient(private val exception: Exception) : StickerPrinterClient {
        override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

        override suspend fun print(address: String, payload: ByteArray) {
            throw exception
        }
    }

    private class DelayingClient : StickerPrinterClient {
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
