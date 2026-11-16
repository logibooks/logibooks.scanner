// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class ParcelStickerRendererTest {
    private val renderer = ParcelStickerRenderer()

    @Test
    fun renderWbrNMatchesReferenceLayoutUsingSticker() {
        val rendered = renderer.render(
            ParcelSticker.WbrN(
                sticker = "39639934424"
            )
        )
        val commands = rendered.toString(WINDOWS_1251)

        assertFalse(commands.contains("BITMAP"))
        assertTrue(commands.contains("TEXT"))
        assertEquals(5, commands.countOccurrences("QRCODE"))
        assertEquals(5, commands.countOccurrences("\"39639934424\""))
        assertTrue(commands.contains("QRCODE 127,55,L,10,A,0,M2,S7,\"39639934424\""))
        assertTrue(commands.contains("QRCODE 20,18,L,4,A,0,M2,S7,\"39639934424\""))
        assertTrue(commands.contains("QRCODE 365,18,L,4,A,0,M2,S7,\"39639934424\""))
        assertTrue(commands.contains("QRCODE 20,218,L,4,A,0,M2,S7,\"39639934424\""))
        assertTrue(commands.contains("QRCODE 365,218,L,4,A,0,M2,S7,\"39639934424\""))
        assertFalse(commands.contains("BARCODE"))
        assertTrue(commands.contains("TEXT 378,160,\"2\",270,1,1,2,\"3963993\""))
        assertTrue(commands.contains("TEXT 417,160,\"4\",270,1,1,2,\"4424\""))
        assertTrue(commands.contains("TEXT 20,208,\"4\",270,2,2,\"WB\""))
        assertTrue(commands.endsWith("PRINT 1,1\r\n"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun renderWbrNRejectsMissingSticker() {
        renderer.render(ParcelSticker.WbrN(null))
    }

    private fun String.countOccurrences(value: String): Int =
        windowed(value.length).count { it == value }

    private companion object {
        val WINDOWS_1251: Charset = Charset.forName("windows-1251")
    }
}
