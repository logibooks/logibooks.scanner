// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlin.math.max

class TscLabelRenderer {

    fun render(code: String): ByteArray = renderCommands(code).toByteArray(Charsets.UTF_8)

    fun renderCommands(code: String): String {
        val value = normalizeCode(code)
        val textX = max(0, (LABEL_WIDTH_DOTS - value.length * TEXT_CHAR_WIDTH_DOTS) / 2)

        return listOf(
            "SIZE ${LABEL_WIDTH_MM} mm,${LABEL_HEIGHT_MM} mm",
            "GAP 2 mm,0 mm",
            "DENSITY 8",
            "DIRECTION 1",
            "REFERENCE 0,0",
            "CLS",
            "QRCODE $QR_X_DOTS,$QR_Y_DOTS,L,$QR_CELL_DOTS,A,0,M2,S7,\"$value\"",
            "TEXT $textX,$TEXT_Y_DOTS,\"3\",0,1,1,\"$value\"",
            "PRINT 1,1"
        ).joinToString("\r\n", postfix = "\r\n")
    }

    private fun normalizeCode(code: String): String {
        val value = code.trim()
        require(value.isNotEmpty()) { "Label code must not be blank" }
        require(value.none { it == '"' || it.code < 0x20 || it.code == 0x7F }) {
            "Label code must not contain quotes or control characters"
        }
        return value
    }

    private companion object {
        const val LABEL_WIDTH_MM = 58
        const val LABEL_HEIGHT_MM = 40

        // RE310/TSPL coordinates are dots; these defaults target 203 dpi label stock.
        const val LABEL_WIDTH_DOTS = 464
        const val QR_X_DOTS = 130
        const val QR_Y_DOTS = 32
        const val QR_CELL_DOTS = 7
        const val TEXT_Y_DOTS = 265
        const val TEXT_CHAR_WIDTH_DOTS = 16
    }
}
