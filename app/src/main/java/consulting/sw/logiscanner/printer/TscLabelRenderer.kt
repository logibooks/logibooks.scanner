// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class TscLabelRenderer(
    private val clock: Clock = Clock.systemDefaultZone()
) {

    fun render(code: String): ByteArray = renderCommands(code).toByteArray(Charsets.UTF_8)

    fun renderFullRelabeling(parcelId: Int, registerId: Int): ByteArray {
        return renderFullRelabelingCommands(parcelId, registerId).toByteArray(Charsets.UTF_8)
    }

    fun renderCommands(code: String): String {
        val value = normalizeCode(code)
        val printedAt = ZonedDateTime.now(clock).format(PRINT_TIME_FORMATTER)

        return listOf(
            "SIZE ${LABEL_WIDTH_MM} mm,${LABEL_HEIGHT_MM} mm",
            "GAP 2 mm,0 mm",
            "DENSITY 8",
            "DIRECTION 1",
            "REFERENCE 0,0",
            "CLS",
            "QRCODE $QR_X_DOTS,$QR_Y_DOTS,L,$QR_CELL_DOTS,A,0,M2,S7,\"$value\"",
            qrCenteredRotatedTextCommand(BRAND_TEXT, BRAND_TEXT_X_DOTS, TEXT_CHAR_WIDTH_DOTS, BRAND_TEXT_ROTATION),
            textCommand(value, NUMBER_TEXT_Y_DOTS, TEXT_CHAR_WIDTH_DOTS * NUMBER_TEXT_SCALE, NUMBER_TEXT_SCALE),
            textCommand(printedAt, DATE_TEXT_Y_DOTS, TEXT_CHAR_WIDTH_DOTS),
            "PRINT 1,1"
        ).joinToString("\r\n", postfix = "\r\n")
    }

    fun renderFullRelabelingCommands(parcelId: Int, registerId: Int): String {
        val parcelCode = paddedPositiveId(parcelId, PARCEL_ID_LABEL_LENGTH, "Parcel id")
        val registerCode = paddedPositiveId(registerId, REGISTER_ID_LABEL_LENGTH, "Register id")
        val printedAt = ZonedDateTime.now(clock).format(PRINT_TIME_FORMATTER)

        return listOf(
            "SIZE ${LABEL_WIDTH_MM} mm,${LABEL_HEIGHT_MM} mm",
            "GAP 2 mm,0 mm",
            "DENSITY 8",
            "DIRECTION 1",
            "REFERENCE 0,0",
            "CLS",
            "QRCODE $QR_X_DOTS,$QR_Y_DOTS,L,$QR_CELL_DOTS,A,0,M2,S7,\"$parcelCode\"",
            qrCenteredRotatedTextCommand(BRAND_TEXT, BRAND_TEXT_X_DOTS, TEXT_CHAR_WIDTH_DOTS, BRAND_TEXT_ROTATION),
            qrCenteredMirroredRotatedTextCommand(
                registerCode,
                REGISTER_TEXT_X_DOTS,
                TEXT_CHAR_WIDTH_DOTS,
                REGISTER_TEXT_ROTATION
            ),
            textCommand(parcelCode, NUMBER_TEXT_Y_DOTS, TEXT_CHAR_WIDTH_DOTS * NUMBER_TEXT_SCALE, NUMBER_TEXT_SCALE),
            textCommand(printedAt, DATE_TEXT_Y_DOTS, TEXT_CHAR_WIDTH_DOTS),
            "PRINT 1,1"
        ).joinToString("\r\n", postfix = "\r\n")
    }

    private fun textCommand(
        value: String,
        y: Int,
        charWidthDots: Int,
        scale: Int = 1
    ): String {
        val x = max(0, (LABEL_WIDTH_DOTS - value.length * charWidthDots) / 2)
        return "TEXT $x,$y,\"3\",0,$scale,$scale,\"$value\""
    }

    private fun rotatedTextCommand(value: String, x: Int, y: Int, rotation: Int): String =
        "TEXT $x,$y,\"3\",$rotation,1,1,\"$value\""

    private fun qrCenteredRotatedTextCommand(value: String, x: Int, charWidthDots: Int, rotation: Int): String {
        val y = QR_Y_DOTS + (QR_SIZE_DOTS + value.length * charWidthDots) / 2
        return rotatedTextCommand(value, x, y, rotation)
    }

    private fun qrCenteredMirroredRotatedTextCommand(value: String, x: Int, charWidthDots: Int, rotation: Int): String {
        val y = QR_Y_DOTS + (QR_SIZE_DOTS - value.length * charWidthDots) / 2
        return rotatedTextCommand(value, x, y, rotation)
    }

    private fun normalizeCode(code: String): String {
        val value = code.trim()
        require(value.isNotEmpty()) { "Label code must not be blank" }
        require(value.none { it == '"' || it.code < 0x20 || it.code == 0x7F }) {
            "Label code must not contain quotes or control characters"
        }
        return value
    }

    private fun paddedPositiveId(value: Int, length: Int, name: String): String {
        require(value > 0) { "$name must be positive" }
        return value.toString().padStart(length, '0')
    }

    private companion object {
        const val LABEL_WIDTH_MM = 58
        const val LABEL_HEIGHT_MM = 40
        val PRINT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "dd.MM.yyyy HH:mm",
            Locale.forLanguageTag("ru-RU")
        )

        // RE310/TSPL coordinates are dots; these defaults target 203 dpi label stock.
        const val LABEL_WIDTH_DOTS = 464
        const val QR_X_DOTS = 148
        const val QR_Y_DOTS = 18
        const val QR_CELL_DOTS = 8
        const val QR_MODULES = 21
        const val QR_SIZE_DOTS = QR_CELL_DOTS * QR_MODULES
        const val TEXT_CHAR_WIDTH_DOTS = 16
        const val BRAND_TEXT_X_DOTS = 50
        const val BRAND_TEXT_ROTATION = 270
        const val REGISTER_TEXT_X_DOTS = 414
        const val REGISTER_TEXT_ROTATION = 90
        const val NUMBER_TEXT_Y_DOTS = 230
        const val DATE_TEXT_Y_DOTS = 288
        const val NUMBER_TEXT_SCALE = 2
        const val BRAND_TEXT = "GTC-Express"
        const val PARCEL_ID_LABEL_LENGTH = 9
        const val REGISTER_ID_LABEL_LENGTH = 6
    }
}
