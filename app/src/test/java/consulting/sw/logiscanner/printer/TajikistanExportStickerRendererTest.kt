// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class TajikistanExportStickerRendererTest {
    private val renderer = TajikistanExportStickerRenderer()

    @Test
    fun renderEmitsVectorTsplAndNativeCode128() {
        val sticker = printableSticker()
        val payload = renderer.render(sticker).toString(WINDOWS_1251)

        assertTrue(payload.startsWith("SIZE 58 mm,40 mm\r\n"))
        assertTrue(payload.contains("CODEPAGE 1251\r\n"))
        assertFalse(payload.contains("BITMAP"))
        assertTrue(payload.contains("TEXT "))
        assertTrue(payload.contains("BAR "))
        assertTrue(payload.contains("BARCODE"))
        assertTrue(payload.contains("\"${sticker.orderNumber}\""))
        assertTrue(payload.contains("Иванов"))
        assertTrue(payload.contains("Стоимость товаров"))
        assertTrue(payload.contains("1200,00 руб."))
        assertFalse(payload.contains("ЦЕННОСТЬ"))
        assertTrue(payload.contains("Дата отгрузки"))
        assertTrue(payload.contains("Вес груза"))
        assertFalse(payload.contains("Дата отгрузки:"))
        assertFalse(payload.contains("Вес груза:"))
        assertFalse(payload.contains(",\"СЧЁТ\""))
        assertTrue(payload.contains("Номер заказа ${sticker.orderNumber}"))
        assertFalse(payload.contains("НОМЕР ЗАКАЗА"))
        assertFalse(payload.contains("№ ЗАКАЗА"))
        assertTrue(payload.contains("TEXT 232,16,\"1\",0,1,1,2,\"Номер лицевого счёта ${sticker.dcBankID}\""))
        assertTrue(payload.contains("Описание содержимого (товаров) и количество"))
        assertFalse(payload.contains(",\"ТОВАРЫ\""))
        assertTrue(payload.contains("TEXT 16,2,\"1\",0,1,1,\"МЕЖДУНАРОДНАЯ ТРАНСПОРТНАЯ НАКЛАДНАЯ\""))
        assertTrue(payload.contains("TEXT 448,2,\"1\",0,1,1,3,\"МЕСТ:1\""))
        assertTrue(payload.contains("TEXT 280,104,\"1\",0,1,1,\"Стоимость товаров\""))
        assertTrue(payload.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderWrapsAndTruncatesLongPartyTextWithoutRejectingSticker() {
        val sticker = printableSticker().copy(
            senderAddress = "Российская Федерация, город Москва, очень длинная улица, дом 1 ".repeat(10),
            recipientAddress = "Республика Таджикистан, город Душанбе, улица Рудаки, дом 100 ".repeat(10)
        )

        val payload = renderer.render(sticker).toString(WINDOWS_1251)

        assertTrue(payload.contains("..."))
        assertTrue(payload.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderPrintsIncompleteStickerAndOmitsUnavailableBarcode() {
        val payload = renderer.render(null.toPrintableSticker()).toString(WINDOWS_1251)

        assertFalse(payload.contains("BITMAP"))
        assertFalse(payload.contains("BARCODE"))
        assertTrue(payload.contains("TEXT "))
        assertTrue(payload.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun renderEmitsBarcodeForLongNumericOrderNumberThatFitsCode128C() {
        val orderNumber = "1234567890123456789012345678"
        val payload = renderer.render(printableSticker().copy(orderNumber = orderNumber))
            .toString(WINDOWS_1251)

        assertTrue(payload.contains("BARCODE"))
        assertTrue(payload.contains("\"$orderNumber\""))
    }

    @Test
    fun renderUsesThreeItemLinesAndKeepsQuantityWhenProductNameIsTruncated() {
        val sticker = printableSticker().copy(
            items = listOf(
                TajikistanExportStickerItem(
                    "Очень длинное наименование товара для проверки печати ".repeat(10),
                    7
                )
            )
        )

        val itemCommands = renderer.render(sticker).toString(WINDOWS_1251)
            .lineSequence()
            .filter { line ->
                line.startsWith("TEXT 16,277,") ||
                    line.startsWith("TEXT 16,289,") ||
                    line.startsWith("TEXT 16,301,")
            }
            .toList()

        assertEquals(3, itemCommands.size)
        assertTrue(itemCommands.last().contains("..."))
        assertTrue(itemCommands.last().contains(" - 7\""))
    }

    @Test
    fun renderStacksFullWidthPartiesAndBreaksLongWordsWithinMargins() {
        val longWord = "А".repeat(220)
        val payloadLines = renderer.render(
            printableSticker().copy(
                senderName = "Sender",
                senderAddress = longWord,
                recipientName = "Recipient",
                recipientAddress = longWord,
                recipientPhone = "+992 900 00 00 00"
            )
        ).toString(WINDOWS_1251).lineSequence().toList()

        val senderHeaderIndex = payloadLines.indexOfFirst { it.contains("Грузоотправитель") }
        val recipientHeaderIndex = payloadLines.indexOfFirst { it.contains("Грузополучатель") }
        val partyLines = payloadLines.filter { line ->
            PARTY_TEXT_Y_DOTS.any { y -> line.startsWith("TEXT 16,$y,") }
        }

        assertTrue(senderHeaderIndex >= 0)
        assertTrue(recipientHeaderIndex > senderHeaderIndex)
        assertEquals(6, partyLines.size)
        assertTrue(partyLines.all { textValue(it).length <= PARTY_MAX_CHARS })
        assertEquals(PARTY_MAX_CHARS, textValue(partyLines[1]).length)
        assertEquals(PARTY_MAX_CHARS, textValue(partyLines[4]).length)
        assertTrue(partyLines[2].contains("..."))
        assertTrue(partyLines[4].contains("..."))
        assertTrue(partyLines[5].contains("+992 900 00 00 00"))
        assertTrue(payloadLines.contains("BAR 16,136,432,1"))
        assertTrue(payloadLines.contains("BAR 16,193,432,1"))
        assertTrue(payloadLines.contains("BAR 16,258,432,1"))
    }

    private fun textValue(command: String): String = command.substringAfterLast(",\"").removeSuffix("\"")

    private companion object {
        const val PARTY_MAX_CHARS = 50
        val PARTY_TEXT_Y_DOTS = listOf(155, 167, 179, 212, 224, 244)
        val WINDOWS_1251: Charset = Charset.forName("windows-1251")
    }
}
