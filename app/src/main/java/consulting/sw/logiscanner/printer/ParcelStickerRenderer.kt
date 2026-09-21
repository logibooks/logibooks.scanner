// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import java.nio.charset.Charset
import java.util.Locale

class ParcelStickerRenderer {

    fun render(sticker: ParcelSticker): ByteArray = when (sticker) {
        is ParcelSticker.WbrN -> renderWbrN(sticker)
        is ParcelSticker.Ozon -> renderOzon(sticker)
    }

    private fun renderWbrN(sticker: ParcelSticker.WbrN): ByteArray {
        val stickerValue = normalizeQr(sticker.sticker)
        require(stickerValue != null) { "WbrN sticker has no printable sticker" }

        val commands = mutableListOf(
            verticalTextCommand(
                value = WBR_BRAND,
                x = 20,
                centerY = 160,
                font = FONT_4,
                charWidthDots = FONT_4_WIDTH_DOTS,
                xMultiplier = 2,
                yMultiplier = 2
            )
        )
        val (first, second) = splitWbrStickerNumber(stickerValue)
        commands += centeredVerticalTextCommand(first, 378, 160, FONT_2)
        if (second.isNotEmpty()) {
            commands += centeredVerticalTextCommand(second, 417, 160, FONT_4)
        }

        commands += listOf(
            qrCommand(CENTRAL_QR_X_DOTS, CENTRAL_QR_Y_DOTS, CENTRAL_QR_CELL_DOTS, stickerValue),
            qrCommand(20, 18, CORNER_QR_CELL_DOTS, stickerValue),
            qrCommand(365, 18, CORNER_QR_CELL_DOTS, stickerValue),
            qrCommand(20, 218, CORNER_QR_CELL_DOTS, stickerValue),
            qrCommand(365, 218, CORNER_QR_CELL_DOTS, stickerValue)
        )
        return buildPayload(commands)
    }

    private fun renderOzon(sticker: ParcelSticker.Ozon): ByteArray {
        val postingNumber = normalizeText(sticker.postingNumber)
        val barcode = normalizeQr(sticker.barcode)
        val destinationCity = normalizeText(sticker.destinationCity)?.uppercase(RUSSIAN_LOCALE)
        require(postingNumber != null || barcode != null || destinationCity != null) {
            "Ozon sticker has no printable data"
        }

        val commands = mutableListOf(
            verticalTextCommand(OZON_BRAND, 31, 160, FONT_4, FONT_4_WIDTH_DOTS)
        )
        destinationCity?.let { city ->
            commands += fittedVerticalTextCommand(city, 435, 160)
        }
        postingNumber?.let { value ->
            val (first, second) = splitPostingNumber(value)
            commands += verticalTextCommand(first, 105, 160, FONT_3, FONT_3_WIDTH_DOTS)
            if (second.isNotEmpty()) {
                commands += verticalTextCommand(second, 350, 160, FONT_3, FONT_3_WIDTH_DOTS)
            }
        }

        barcode?.let { commands += qrCommand(169, 94, 6, it) }
        return buildPayload(commands)
    }

    private fun fittedVerticalTextCommand(
        value: String,
        x: Int,
        centerY: Int
    ): String {
        val (font, charWidth) = when {
            value.length * FONT_3_WIDTH_DOTS <= MAX_VERTICAL_TEXT_LENGTH_DOTS -> FONT_3 to FONT_3_WIDTH_DOTS
            value.length * FONT_2_WIDTH_DOTS <= MAX_VERTICAL_TEXT_LENGTH_DOTS -> FONT_2 to FONT_2_WIDTH_DOTS
            else -> FONT_1 to FONT_1_WIDTH_DOTS
        }
        return verticalTextCommand(value, x, centerY, font, charWidth)
    }

    private fun verticalTextCommand(
        value: String,
        x: Int,
        centerY: Int,
        font: String,
        charWidthDots: Int,
        xMultiplier: Int = 1,
        yMultiplier: Int = 1
    ): String {
        val y = centerY + value.length * charWidthDots * xMultiplier / 2
        return "TEXT $x,$y,\"$font\",270,$xMultiplier,$yMultiplier,\"$value\""
    }

    private fun centeredVerticalTextCommand(
        value: String,
        x: Int,
        centerY: Int,
        font: String
    ): String = "TEXT $x,$centerY,\"$font\",270,1,1,2,\"$value\""

    private fun splitInHalf(value: String): Pair<String, String> {
        val midpoint = (value.length + 1) / 2
        return value.substring(0, midpoint) to value.substring(midpoint)
    }

    private fun splitWbrStickerNumber(value: String): Pair<String, String> = when {
        value.length > WBR_NUMBER_SUFFIX_LENGTH ->
            value.dropLast(WBR_NUMBER_SUFFIX_LENGTH) to value.takeLast(WBR_NUMBER_SUFFIX_LENGTH)
        else -> splitInHalf(value)
    }

    private fun splitPostingNumber(value: String): Pair<String, String> {
        val separator = value.indexOf('-').takeIf { it in 1 until value.lastIndex }
        return if (separator != null) {
            value.substring(0, separator) to value.substring(separator + 1)
        } else {
            splitInHalf(value)
        }
    }

    private fun normalizeText(value: String?): String? = value
        ?.trim()
        ?.takeIf { text ->
            text.isNotEmpty()
                && '"' !in text
                && text.none { it.code < 0x20 || it.code == 0x7F }
                && WINDOWS_1251.newEncoder().canEncode(text)
        }

    private fun normalizeQr(value: String?): String? = normalizeText(value)
        ?.takeIf { it.all { char -> char.code <= 0x7E } }

    private fun qrCommand(x: Int, y: Int, cellDots: Int, value: String): String =
        "QRCODE $x,$y,L,$cellDots,A,0,M2,S7,\"$value\""

    private fun buildPayload(commands: List<String>): ByteArray = buildList {
        addAll(
            listOf(
                "SIZE 58 mm,40 mm",
                "GAP 2 mm,0 mm",
                "DENSITY 8",
                "DIRECTION 1",
                "REFERENCE 0,0",
                "CODEPAGE 1251",
                "CLS"
            )
        )
        addAll(commands)
        add("PRINT 1,1")
    }.joinToString("\r\n", postfix = "\r\n").toByteArray(WINDOWS_1251)

    companion object {
        private const val WBR_BRAND = "WB"
        private const val OZON_BRAND = "OZON"
        private const val FONT_1 = "1"
        private const val FONT_2 = "2"
        private const val FONT_3 = "3"
        private const val FONT_4 = "4"
        private const val FONT_1_WIDTH_DOTS = 8
        private const val FONT_2_WIDTH_DOTS = 12
        private const val FONT_3_WIDTH_DOTS = 16
        private const val FONT_4_WIDTH_DOTS = 24
        private const val MAX_VERTICAL_TEXT_LENGTH_DOTS = 270
        private const val WBR_NUMBER_SUFFIX_LENGTH = 4
        private const val CENTRAL_QR_CELL_DOTS = 10
        private const val CENTRAL_QR_SIZE_DOTS = 21 * CENTRAL_QR_CELL_DOTS
        private const val CENTRAL_QR_X_DOTS = (464 - CENTRAL_QR_SIZE_DOTS) / 2
        private const val CENTRAL_QR_Y_DOTS = (320 - CENTRAL_QR_SIZE_DOTS) / 2
        private const val CORNER_QR_CELL_DOTS = 4
        private val WINDOWS_1251: Charset = Charset.forName("windows-1251")
        private val RUSSIAN_LOCALE = Locale.forLanguageTag("ru-RU")
    }
}
