// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TscLabelRendererTest {

    private val renderer = TscLabelRenderer(
        Clock.fixed(
            Instant.parse("2026-06-23T10:05:30Z"),
            ZoneId.of("Europe/Moscow")
        )
    )

    @Test
    fun renderCommandsBuilds58By40QrLabel() {
        val commands = renderer.renderCommands("KGT-15")

        assertTrue(commands.contains("SIZE 58 mm,40 mm"))
        assertTrue(commands.contains("QRCODE 148,18,L,8,A,0,M2,S7,\"KGT-15\""))
        assertTrue(commands.contains("TEXT 50,190,\"3\",270,1,1,\"GTC-Express\""))
        assertTrue(commands.contains("TEXT 136,230,\"3\",0,2,2,\"KGT-15\""))
        assertTrue(commands.contains("TEXT 104,288,\"3\",0,1,1,\"23.06.2026 13:05\""))
        assertTrue(commands.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderFullRelabelingCommandsBuildsParcelAndRegisterLabel() {
        val commands = renderer.renderFullRelabelingCommands(parcelId = 123, registerId = 45)

        assertTrue(commands.contains("SIZE 58 mm,40 mm"))
        assertTrue(commands.contains("QRCODE 148,18,L,8,A,0,M2,S7,\"000000123\""))
        assertTrue(commands.contains("TEXT 50,190,\"3\",270,1,1,\"GTC-Express\""))
        assertTrue(commands.contains("TEXT 414,54,\"3\",90,1,1,\"000045\""))
        assertTrue(commands.contains("TEXT 88,230,\"3\",0,2,2,\"000000123\""))
        assertTrue(commands.contains("TEXT 104,288,\"3\",0,1,1,\"23.06.2026 13:05\""))
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

    @Test
    fun renderFullRelabelingCommandsRejectsNonPositiveIds() {
        assertThrows(IllegalArgumentException::class.java) {
            renderer.renderFullRelabelingCommands(parcelId = 0, registerId = 45)
        }
        assertThrows(IllegalArgumentException::class.java) {
            renderer.renderFullRelabelingCommands(parcelId = 123, registerId = 0)
        }
    }
}
