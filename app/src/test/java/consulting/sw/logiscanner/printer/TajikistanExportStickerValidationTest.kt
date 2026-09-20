// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import consulting.sw.logiscanner.net.TajikistanExportStickerItemPayload
import consulting.sw.logiscanner.net.TajikistanExportStickerPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TajikistanExportStickerValidationTest {

    @Test
    fun validPayloadBuildsPrintableStickerAndTrimsText() {
        val sticker = payload().copy(orderNumber = " 40856360164 ").toPrintableSticker()

        assertEquals("40856360164", sticker.orderNumber)
        assertEquals(2, sticker.items.single().quantity)
    }

    @Test
    fun missingPayloadOrRequiredFieldBuildsIncompletePrintableSticker() {
        val empty = null.toPrintableSticker()
        val partial = payload().copy(accountNumber = null, items = emptyList()).toPrintableSticker()

        assertEquals("", empty.orderNumber)
        assertEquals("", empty.accountNumber)
        assertEquals(null, empty.weightKg)
        assertEquals("", partial.accountNumber)
        assertEquals(emptyList<TajikistanExportStickerItem>(), partial.items)
    }

    @Test
    fun invalidNumericValuesBecomeBlankWithoutBlockingPrint() {
        val sticker = payload().copy(
            placesCount = 0,
            weightKg = Double.NaN,
            declaredValue = 0.0,
            items = listOf(TajikistanExportStickerItemPayload(null, 0))
        ).toPrintableSticker()

        assertEquals(null, sticker.placesCount)
        assertEquals(null, sticker.weightKg)
        assertEquals(null, sticker.declaredValue)
        assertEquals("", sticker.items.single().description)
        assertEquals(null, sticker.items.single().quantity)
    }

    @Test
    fun unsupportedCode128OrderNumberIsKeptForRasterTextButNotBarcode() {
        listOf("Заказ", "bad\"value", "ABCDEFGHIJKLMNOP").forEach { orderNumber ->
            val sticker = payload().copy(orderNumber = orderNumber).toPrintableSticker()
            assertEquals(orderNumber, sticker.orderNumber)
            assertFalse(isSupportedCode128Value(sticker.orderNumber))
        }
    }

    @Test
    fun code128WidthUsesNumericCompactionAndPreservesQuietZones() {
        assertTrue(isSupportedCode128Value("123456789012345678901234567890"))
        assertFalse(isSupportedCode128Value("1234567890123456789012345678901"))
    }
}

internal fun payload() = TajikistanExportStickerPayload(
    orderNumber = "40856360164",
    accountNumber = "ACC-42",
    placesCount = 1,
    dispatchDate = "19.09.26",
    weightKg = 1.25,
    declaredValue = 1200.0,
    currency = "RUB",
    senderName = "Sender",
    senderAddress = "123456, Moscow, Tverskaya 1",
    recipientName = "Иванов Иван",
    recipientAddress = "Душанбе, Рудаки 1",
    recipientPhone = "+992 900 00 00 00",
    items = listOf(TajikistanExportStickerItemPayload("Книги", 2))
)

internal fun printableSticker(): TajikistanExportSticker =
    payload().toPrintableSticker()
