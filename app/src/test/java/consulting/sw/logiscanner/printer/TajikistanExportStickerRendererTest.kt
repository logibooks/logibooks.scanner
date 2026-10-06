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
        assertTrue(payload.contains("TEXT 16,198,\"1\",0,1,1,\"Грузополучатель\""))
        assertFalse(payload.contains("TEXT 16,212,"))
        assertTrue(payload.contains("TEXT 16,224,\"1\",0,1,1,\"${sticker.recipientAddress}\""))
        assertTrue(payload.contains("TEXT 16,244,\"1\",0,1,1,\"${sticker.recipientPhone}\""))
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
        assertEquals(5, partyLines.size)
        assertTrue(partyLines.all { textValue(it).length <= PARTY_MAX_CHARS })
        assertEquals(TJ_STICKER_TEXT_LINE_MAX_CHARS, textValue(partyLines[1]).length)
        assertEquals(TJ_STICKER_TEXT_LINE_MAX_CHARS, textValue(partyLines[3]).length)
        assertTrue(partyLines[2].contains("..."))
        assertTrue(partyLines[3].contains("..."))
        assertTrue(partyLines[4].contains("+992 900 00 00 00"))
        assertTrue(payloadLines.contains("BAR 16,136,432,1"))
        assertTrue(payloadLines.contains("BAR 16,193,432,1"))
        assertTrue(payloadLines.contains("BAR 16,258,432,1"))
    }

    @Test
    fun addressesWrapAt42AndRecipientOverflowIncludesEllipsisWithin42Characters() {
        listOf('A', 'А').forEach { character ->
            listOf(42, 43).forEach { length ->
                val address = character.toString().repeat(length)
                val sticker = printableSticker().copy(senderName = "Sender", senderAddress = address, recipientAddress = address)
                val sender = valuesAt(sticker, listOf(155, 167, 179))
                assertEquals(listOf("Sender") + address.chunked(42), sender)
                val recipient = valuesAt(sticker, listOf(224)).single()
                assertEquals(if (length == 42) address else address.take(39) + "...", recipient)
                assertEquals(42, recipient.length)
            }
        }
        val address = "Республика Таджикистан, город Душанбе, улица Рудаки, дом 100"
        val recipient = valuesAt(printableSticker().copy(recipientAddress = address), listOf(224)).single()
        assertTrue(recipient.endsWith("..."))
        assertTrue(recipient.length <= 42)
    }

    @Test
    fun senderKeepsFieldLimitsAndAllocatesThreeRowsInNameThenAddressOrder() {
        val address = "А".repeat(85)
        val sticker = printableSticker().copy(senderName = "И".repeat(51), senderAddress = address)
        assertEquals(
            listOf("И".repeat(50), "И", "А".repeat(39) + "..."),
            valuesAt(sticker, listOf(155, 167, 179))
        )
        assertEquals(
            listOf("И".repeat(50), "И".repeat(50), "И".repeat(47) + "..."),
            valuesAt(sticker.copy(senderName = "И".repeat(150)), listOf(155, 167, 179))
        )
        assertEquals(
            listOf("И".repeat(50), "А".repeat(42), "А".repeat(39) + "..."),
            valuesAt(sticker.copy(senderName = "И".repeat(50)), listOf(155, 167, 179))
        )
        listOf(50, 51).forEach { length ->
            val phone = "1".repeat(length)
            assertEquals(
                if (length == 50) phone else "1".repeat(47) + "...",
                valuesAt(sticker.copy(recipientPhone = phone), listOf(244)).single()
            )
        }
    }

    @Test
    fun productBoundaryLinesStayWithin42CharactersAndKeepQuantities() {
        listOf('A', 'А').forEach { character ->
            listOf(42, 43).forEach { length ->
                val name = character.toString().repeat(length)
                val lines = valuesAt(
                    printableSticker().copy(items = listOf(TajikistanExportStickerItem(name, 7))),
                    listOf(277, 289, 301)
                )
                assertTrue(lines.all { it.length <= 42 })
                assertTrue(lines.last().endsWith(" - 7"))
                if (length == 42) {
                    assertEquals(listOf(name.take(35) + "... - 7"), lines)
                } else {
                    assertEquals(listOf(name.take(42), "$character - 7"), lines)
                }
            }
        }
    }

    private fun valuesAt(sticker: TajikistanExportSticker, ys: List<Int>): List<String> =
        renderer.render(sticker).toString(WINDOWS_1251).lineSequence()
            .filter { command -> ys.any { command.startsWith("TEXT 16,$it,") } }
            .map(::textValue).toList()

    private fun textValue(command: String): String = command.substringAfterLast(",\"").removeSuffix("\"")

    private companion object {
        const val PARTY_MAX_CHARS = 50
        val PARTY_TEXT_Y_DOTS = listOf(155, 167, 179, 224, 244)
        val WINDOWS_1251: Charset = Charset.forName("windows-1251")
    }
}
