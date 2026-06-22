// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TscLabelRendererTest {

    private val renderer = TscLabelRenderer()

    @Test
    fun renderCommandsBuilds58By40QrLabel() {
        val commands = renderer.renderCommands("KGT-15")

        assertTrue(commands.contains("SIZE 58 mm,40 mm"))
        assertTrue(commands.contains("QRCODE 130,32,L,7,A,0,M2,S7,\"KGT-15\""))
        assertTrue(commands.contains("TEXT "))
        assertTrue(commands.contains("\"KGT-15\""))
        assertTrue(commands.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderCommandsRejectsBlankCode() {
        assertThrows(IllegalArgumentException::class.java) {
            renderer.renderCommands(" ")
        }
    }

    @Test
    fun renderCommandsRejectsQuotesAndControlCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            renderer.renderCommands("KGT\"15")
        }
        assertThrows(IllegalArgumentException::class.java) {
            renderer.renderCommands("KGT\n15")
        }
    }
}
