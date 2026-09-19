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
class ParcelLabelRendererTest {
    private val renderer = ParcelLabelRenderer()

    @Test
    fun renderWbrNEmitsFiveStickerCodeQrsAndNoLinearBarcode() {
        val rendered = renderer.render(
            ParcelLabel.WbrN(
                sticker = "54178953272",
                stickerCode = "*DJ1RODh1"
            )
        )
        val commands = commandsAfterBitmap(rendered)

        assertRasterContainsInk(rendered)
        assertEquals(5, commands.countOccurrences("QRCODE"))
        assertEquals(5, commands.countOccurrences("\"*DJ1RODh1\""))
        assertFalse(commands.contains("BARCODE"))
        assertTrue(commands.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderWbrNSupportsEitherAvailableField() {
        val stickerOnly = commandsAfterBitmap(renderer.render(ParcelLabel.WbrN("54178953272", null)))
        val qrOnly = commandsAfterBitmap(renderer.render(ParcelLabel.WbrN(null, "*DJ1RODh1")))

        assertFalse(stickerOnly.contains("QRCODE"))
        assertEquals(5, qrOnly.countOccurrences("QRCODE"))
    }

    @Test
    fun renderOzonEmitsBarcodeQrAndRasterizesDestination() {
        val rendered = renderer.render(
            ParcelLabel.Ozon(
                postingNumber = "0216094457-0039-1",
                barcode = "ii16065571612",
                destinationCity = "Ташкент"
            )
        )
        val commands = commandsAfterBitmap(rendered)

        assertRasterContainsInk(rendered)
        assertEquals(1, commands.countOccurrences("QRCODE"))
        assertTrue(commands.contains("\"ii16065571612\""))
        assertFalse(commands.contains("Ташкент"))
        assertTrue(commands.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderOzonOmitsInvalidQrAndPrintsRemainingData() {
        val commands = commandsAfterBitmap(
            renderer.render(ParcelLabel.Ozon("POST-1", "bad\"barcode", null))
        )

        assertFalse(commands.contains("QRCODE"))
        assertTrue(commands.endsWith("PRINT 1,1\r\n"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun renderRejectsLabelWithoutPrintableData() {
        renderer.render(ParcelLabel.Ozon(" ", null, "\u0001"))
    }

    private fun assertRasterContainsInk(rendered: ByteArray) {
        val header = "BITMAP 0,0,58,320,0,".toByteArray(Charsets.US_ASCII)
        val headerIndex = rendered.indexOf(header)
        val rasterStart = headerIndex + header.size
        val raster = rendered.copyOfRange(
            rasterStart,
            rasterStart + ParcelLabelRenderer.RASTER_SIZE_BYTES
        )

        assertTrue(headerIndex > 0)
        assertTrue(raster.any { it.toInt() != 0 })
    }

    private fun commandsAfterBitmap(rendered: ByteArray): String {
        val header = "BITMAP 0,0,58,320,0,".toByteArray(Charsets.US_ASCII)
        val rasterStart = rendered.indexOf(header) + header.size
        return rendered.copyOfRange(
            rasterStart + ParcelLabelRenderer.RASTER_SIZE_BYTES,
            rendered.size
        ).toString(Charsets.US_ASCII)
    }

    private fun String.countOccurrences(value: String): Int =
        windowed(value.length).count { it == value }

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
