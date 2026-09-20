// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import java.nio.charset.Charset
import java.util.Locale

class TajikistanExportStickerRenderer {

    fun render(sticker: TajikistanExportSticker): ByteArray {
        val commands = mutableListOf<String>()

        commands += text(CONTENT_LEFT_DOTS, TITLE_Y_DOTS, TITLE)
        commands += rightAlignedText("МЕСТ:${sticker.placesCount ?: ""}", TITLE_Y_DOTS)
        commands += centeredText(
            fitSingleLine("Номер лицевого счёта ${sticker.dcBankID}", FULL_LINE_MAX_CHARS),
            ACCOUNT_Y_DOTS,
        )

        code128SymbolWidthDots(sticker.orderNumber)?.let { width ->
            commands += barcodeCommand(sticker.orderNumber, width)
        }
        commands += centeredText(
            fitSingleLine("Номер заказа ${sticker.orderNumber}", FULL_LINE_MAX_CHARS),
            ORDER_Y_DOTS
        )
        commands += horizontalLine(ORDER_SEPARATOR_Y_DOTS)

        val metadata = listOf(
            "Дата отгрузки" to sticker.dispatchDate,
            "Вес груза" to sticker.weightKg?.let { "${decimal(it)} кг" }.orEmpty(),
            "Стоимость товаров" to sticker.costRub?.let { value -> "${decimal(value)} руб." }.orEmpty()
        )
        metadata.forEachIndexed { index, (header, value) ->
            val x = METADATA_LEFT_DOTS + index * METADATA_COLUMN_WIDTH_DOTS -
                if (index == COST_COLUMN_INDEX) COST_COLUMN_LEFT_SHIFT_DOTS else 0
            commands += text(x, METADATA_HEADER_Y_DOTS, header)
            commands += wrappedText(
                value = value,
                x = x,
                firstY = METADATA_VALUE_Y_DOTS,
                maxChars = METADATA_MAX_CHARS,
                maxLines = 2
            )
        }
        commands += horizontalLine(METADATA_SEPARATOR_Y_DOTS)

        commands += text(CONTENT_LEFT_DOTS, SENDER_HEADER_Y_DOTS, "Грузоотправитель")
        commands += wrappedText(
            value = listOf(sticker.senderName, sticker.senderAddress).joinToString("\n"),
            x = CONTENT_LEFT_DOTS,
            firstY = SENDER_VALUE_Y_DOTS,
            maxChars = PARTY_MAX_CHARS,
            maxLines = SENDER_MAX_LINES
        )
        commands += horizontalLine(SENDER_SEPARATOR_Y_DOTS)

        commands += text(CONTENT_LEFT_DOTS, RECIPIENT_HEADER_Y_DOTS, "Грузополучатель")
        commands += wrappedText(
            value = listOf(sticker.recipientName, sticker.recipientAddress).joinToString("\n"),
            x = CONTENT_LEFT_DOTS,
            firstY = RECIPIENT_VALUE_Y_DOTS,
            maxChars = PARTY_MAX_CHARS,
            maxLines = RECIPIENT_MAX_LINES
        )
        if (sticker.recipientPhone.isNotBlank()) {
            commands += text(
                CONTENT_LEFT_DOTS,
                RECIPIENT_PHONE_Y_DOTS,
                fitSingleLine(sticker.recipientPhone, PARTY_MAX_CHARS)
            )
        }
        commands += horizontalLine(PARTY_SEPARATOR_Y_DOTS)

        commands += text(
            CONTENT_LEFT_DOTS,
            ITEMS_HEADER_Y_DOTS,
            "Описание содержимого (товаров) и количество"
        )
        itemLines(sticker.items).forEachIndexed { index, line ->
            commands += text(CONTENT_LEFT_DOTS, ITEMS_VALUE_Y_DOTS + index * LINE_HEIGHT_DOTS, line)
        }

        return buildPayload(commands)
    }

    private fun centeredText(value: String, y: Int): String =
        "TEXT $STICKER_CENTER_X_DOTS,$y,\"1\",0,1,1,2,\"${escape(value)}\""

    private fun rightAlignedText(value: String, y: Int): String =
        "TEXT $CONTENT_RIGHT_DOTS,$y,\"1\",0,1,1,3,\"${escape(value)}\""

    private fun text(x: Int, y: Int, value: String): String =
        "TEXT $x,$y,\"1\",0,1,1,\"${escape(value)}\""

    private fun wrappedText(
        value: String,
        x: Int,
        firstY: Int,
        maxChars: Int,
        maxLines: Int
    ): List<String> = wrapAndTruncate(value, maxChars, maxLines).mapIndexed { index, line ->
        text(x, firstY + index * LINE_HEIGHT_DOTS, line)
    }

    private fun wrapAndTruncate(value: String, maxChars: Int, maxLines: Int): List<String> {
        val lines = wrap(value, maxChars)
        if (lines.size <= maxLines) {
            return lines
        }
        return lines.take(maxLines).toMutableList().also { fitted ->
            fitted[fitted.lastIndex] = indicateTruncation(fitted.last(), maxChars)
        }
    }

    private fun wrap(value: String, maxChars: Int): List<String> {
        val lines = mutableListOf<String>()
        sanitize(value).lines().forEach { paragraph ->
            if (paragraph.isBlank()) {
                return@forEach
            }
            var current = ""
            paragraph.split(Regex("\\s+")).forEach { word ->
                word.chunked(maxChars).forEach { piece ->
                    val candidate = if (current.isEmpty()) piece else "$current $piece"
                    if (candidate.length <= maxChars) {
                        current = candidate
                    } else {
                        lines += current
                        current = piece
                    }
                }
            }
            if (current.isNotEmpty()) {
                lines += current
            }
        }
        return lines
    }

    private fun itemLines(items: List<TajikistanExportStickerItem>): List<String> {
        val printableItems = items.filter { item -> item.productName.isNotBlank() || item.quantity != null }
        val lines = mutableListOf<String>()
        printableItems.forEachIndexed { itemIndex, item ->
            if (lines.size >= ITEMS_MAX_LINES) {
                return@forEachIndexed
            }
            val quantitySuffix = item.quantity?.let { " - $it" }.orEmpty()
            val productNameLines = wrap(item.productName, ITEMS_MAX_CHARS).ifEmpty { listOf("") }
            val availableLines = ITEMS_MAX_LINES - lines.size
            val fittedProductName = productNameLines.take(availableLines).toMutableList()
            val contentWasTruncated = productNameLines.size > availableLines ||
                (productNameLines.size == availableLines && itemIndex < printableItems.lastIndex)
            var finalLine = fittedProductName.last()
            if (contentWasTruncated) {
                finalLine = indicateTruncation(
                    finalLine,
                    (ITEMS_MAX_CHARS - quantitySuffix.length).coerceAtLeast(1)
                )
            }
            if (quantitySuffix.isNotEmpty()) {
                val productNameLimit = (ITEMS_MAX_CHARS - quantitySuffix.length).coerceAtLeast(1)
                finalLine = fitSingleLine(finalLine, productNameLimit) + quantitySuffix
            }
            fittedProductName[fittedProductName.lastIndex] = finalLine.take(ITEMS_MAX_CHARS)
            lines += fittedProductName
        }
        return lines.take(ITEMS_MAX_LINES)
    }

    private fun fitSingleLine(value: String, maxChars: Int): String {
        val normalized = sanitize(value).replace(Regex("\\s+"), " ").trim()
        return if (normalized.length <= maxChars) normalized else ellipsize(normalized, maxChars)
    }

    private fun ellipsize(value: String, maxChars: Int): String {
        if (value.length <= maxChars) {
            return value
        }
        if (maxChars <= ELLIPSIS.length) {
            return ELLIPSIS.take(maxChars)
        }
        return value.take(maxChars - ELLIPSIS.length).trimEnd() + ELLIPSIS
    }

    private fun indicateTruncation(value: String, maxChars: Int): String {
        if (maxChars <= ELLIPSIS.length) {
            return ELLIPSIS.take(maxChars)
        }
        return value.removeSuffix(ELLIPSIS)
            .take(maxChars - ELLIPSIS.length)
            .trimEnd() + ELLIPSIS
    }

    private fun sanitize(value: String): String = value
        .replace('"', '\'')
        .map { character ->
            when {
                character == '\n' -> '\n'
                character == '\r' || character == '\t' -> ' '
                character.code < 0x20 || character.code == 0x7F -> ' '
                else -> character
            }
        }
        .joinToString("")

    private fun escape(value: String): String = sanitize(value).replace("\n", " ")

    private fun decimal(value: Double): String = String.format(RUSSIAN_LOCALE, "%.2f", value)

    private fun horizontalLine(y: Int): String = "BAR $CONTENT_LEFT_DOTS,$y,$CONTENT_WIDTH_DOTS,1"

    private fun barcodeCommand(value: String, width: Int): String {
        val x = (STICKER_WIDTH_DOTS - width) / 2
        return "BARCODE $x,$BARCODE_Y_DOTS,\"128\",$BARCODE_HEIGHT_DOTS,0,0," +
            "$BARCODE_NARROW_DOTS,$BARCODE_WIDE_DOTS,\"$value\""
    }

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

    private companion object {
        const val STICKER_WIDTH_DOTS = 464
        const val STICKER_CENTER_X_DOTS = STICKER_WIDTH_DOTS / 2
        const val CONTENT_LEFT_DOTS = 16
        const val CONTENT_WIDTH_DOTS = 432
        const val CONTENT_RIGHT_DOTS = CONTENT_LEFT_DOTS + CONTENT_WIDTH_DOTS
        const val LINE_HEIGHT_DOTS = 12

        const val TITLE_Y_DOTS = 2
        const val ACCOUNT_Y_DOTS = 16
        const val BARCODE_Y_DOTS = 34
        const val BARCODE_HEIGHT_DOTS = 44
        const val BARCODE_NARROW_DOTS = 2
        const val BARCODE_WIDE_DOTS = 2
        const val ORDER_Y_DOTS = 82
        const val ORDER_SEPARATOR_Y_DOTS = 98
        const val FULL_LINE_MAX_CHARS = CONTENT_WIDTH_DOTS / 8

        const val METADATA_LEFT_DOTS = CONTENT_LEFT_DOTS
        const val METADATA_COLUMN_WIDTH_DOTS = CONTENT_WIDTH_DOTS / 3
        const val METADATA_MAX_CHARS = 17
        const val METADATA_HEADER_Y_DOTS = 104
        const val METADATA_VALUE_Y_DOTS = 118
        const val METADATA_SEPARATOR_Y_DOTS = 136

        const val PARTY_MAX_CHARS = 50
        const val SENDER_MAX_LINES = 3
        const val RECIPIENT_MAX_LINES = 2
        const val SENDER_HEADER_Y_DOTS = 141
        const val SENDER_VALUE_Y_DOTS = 155
        const val SENDER_SEPARATOR_Y_DOTS = 193
        const val RECIPIENT_HEADER_Y_DOTS = 198
        const val RECIPIENT_VALUE_Y_DOTS = 212
        const val RECIPIENT_PHONE_Y_DOTS = 244
        const val PARTY_SEPARATOR_Y_DOTS = 258

        const val ITEMS_HEADER_Y_DOTS = 263
        const val ITEMS_VALUE_Y_DOTS = 277
        const val ITEMS_MAX_CHARS = CONTENT_WIDTH_DOTS / 8
        const val ITEMS_MAX_LINES = 3
        const val ELLIPSIS = "..."
        const val COST_COLUMN_INDEX = 2
        const val COST_COLUMN_LEFT_SHIFT_DOTS = 24

        const val TITLE = "МЕЖДУНАРОДНАЯ ТРАНСПОРТНАЯ НАКЛАДНАЯ"
        val WINDOWS_1251: Charset = Charset.forName("windows-1251")
        val RUSSIAN_LOCALE: Locale = Locale.forLanguageTag("ru-RU")
    }
}

class TajikistanStickerOverflowException : IllegalArgumentException("Sticker content does not fit")
