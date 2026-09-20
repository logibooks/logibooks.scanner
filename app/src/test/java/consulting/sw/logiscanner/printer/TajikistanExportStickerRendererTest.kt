// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TajikistanExportStickerRendererTest {
    private val renderer = TajikistanExportStickerRenderer()

    @Test
    fun renderEmitsFixedBitmapAndNativeCode128Barcode() {
        val sticker = printableSticker()
        val rendered = renderer.render(sticker)
        val bitmapHeader = "BITMAP 0,0,58,320,0,".toByteArray(Charsets.US_ASCII)
        val bitmapHeaderIndex = rendered.indexOf(bitmapHeader)
        val rasterStart = bitmapHeaderIndex + bitmapHeader.size
        val raster = rendered.copyOfRange(
            rasterStart,
            rasterStart + TajikistanExportStickerRenderer.RASTER_SIZE_BYTES
        )
        val commandsBeforeBitmap = rendered.copyOfRange(0, bitmapHeaderIndex).toString(Charsets.US_ASCII)
        val commandsAfterBitmap = rendered.copyOfRange(
            rasterStart + TajikistanExportStickerRenderer.RASTER_SIZE_BYTES,
            rendered.size
        ).toString(Charsets.US_ASCII)

        assertTrue(bitmapHeaderIndex > 0)
        assertEquals(18_560, raster.size)
        assertTrue(raster.any { it.toInt() != 0 })
        assertTrue(commandsBeforeBitmap.contains("SIZE 58 mm,40 mm\r\n"))
        assertTrue(commandsBeforeBitmap.contains("DENSITY 8\r\n"))
        assertTrue(commandsAfterBitmap.contains("BARCODE"))
        assertTrue(commandsAfterBitmap.contains("\"${sticker.orderNumber}\""))
        assertTrue(commandsAfterBitmap.endsWith("PRINT 1,1\r\n"))
        assertFalse(commandsBeforeBitmap.contains("TEXT"))
        assertFalse(commandsAfterBitmap.contains("Иванов"))
    }

    @Test
    fun renderSupportsMultipleItemsAndWrappedCyrillic() {
        val sticker = printableSticker().copy(
            senderAddress = "Российская Федерация, город Москва, очень длинная улица, дом 1",
            recipientAddress = "Республика Таджикистан, город Душанбе, длинная улица Рудаки, дом 100",
            items = listOf(
                TajikistanExportStickerItem("Детские книги", 2),
                TajikistanExportStickerItem("Тетради", 3)
            )
        )

        val rendered = renderer.render(sticker)

        assertTrue(rendered.size > TajikistanExportStickerRenderer.RASTER_SIZE_BYTES)
    }

    @Test
    fun renderPrintsIncompleteStickerAndOmitsUnavailableBarcode() {
        val rendered = renderer.render(null.toPrintableSticker())
        val payloadText = rendered.toString(Charsets.ISO_8859_1)

        assertTrue(payloadText.contains("BITMAP 0,0,58,320,0,"))
        assertFalse(payloadText.contains("BARCODE"))
        assertTrue(payloadText.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderEmitsBarcodeForLongNumericOrderNumberThatFitsCode128C() {
        val orderNumber = "123456789012345678901234567890"
        val rendered = renderer.render(printableSticker().copy(orderNumber = orderNumber))
        val payloadText = rendered.toString(Charsets.ISO_8859_1)

        assertTrue(payloadText.contains("BARCODE"))
        assertTrue(payloadText.contains("\"$orderNumber\""))
    }

    @Test(expected = TajikistanStickerOverflowException::class)
    fun renderRejectsContentThatCannotFit() {
        renderer.render(
            printableSticker().copy(
                items = listOf(TajikistanExportStickerItem("Очень длинное описание ".repeat(100), 1))
            )
        )
    }

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        if (needle.isEmpty()) return 0
        for (index in 0..size - needle.size) {
            if (needle.indices.all { offset -> this[index + offset] == needle[offset] }) {
                return index
            }
        }
        return -1
    }
}
