// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import consulting.sw.logiscanner.net.TajikistanExportLabelItemPayload
import consulting.sw.logiscanner.net.TajikistanExportLabelPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TajikistanExportLabelValidationTest {

    @Test
    fun validPayloadBuildsPrintableLabelAndTrimsText() {
        val label = payload().copy(orderNumber = " 40856360164 ").toPrintableLabel()

        assertEquals("40856360164", label.orderNumber)
        assertEquals(2, label.items.single().quantity)
    }

    @Test
    fun missingPayloadOrRequiredFieldBuildsIncompletePrintableLabel() {
        val empty = null.toPrintableLabel()
        val partial = payload().copy(accountNumber = null, items = emptyList()).toPrintableLabel()

        assertEquals("", empty.orderNumber)
        assertEquals("", empty.accountNumber)
        assertEquals(null, empty.weightKg)
        assertEquals("", partial.accountNumber)
        assertEquals(emptyList<TajikistanExportLabelItem>(), partial.items)
    }

    @Test
    fun invalidNumericValuesBecomeBlankWithoutBlockingPrint() {
        val label = payload().copy(
            placesCount = 0,
            weightKg = Double.NaN,
            declaredValue = 0.0,
            items = listOf(TajikistanExportLabelItemPayload(null, 0))
        ).toPrintableLabel()

        assertEquals(null, label.placesCount)
        assertEquals(null, label.weightKg)
        assertEquals(null, label.declaredValue)
        assertEquals("", label.items.single().description)
        assertEquals(null, label.items.single().quantity)
    }

    @Test
    fun unsupportedCode128OrderNumberIsKeptForRasterTextButNotBarcode() {
        listOf("Заказ", "bad\"value", "ABCDEFGHIJKLMNOP").forEach { orderNumber ->
            val label = payload().copy(orderNumber = orderNumber).toPrintableLabel()
            assertEquals(orderNumber, label.orderNumber)
            assertFalse(isSupportedCode128Value(label.orderNumber))
        }
    }

    @Test
    fun code128WidthUsesNumericCompactionAndPreservesQuietZones() {
        assertTrue(isSupportedCode128Value("123456789012345678901234567890"))
        assertFalse(isSupportedCode128Value("1234567890123456789012345678901"))
    }
}

internal fun payload() = TajikistanExportLabelPayload(
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
    items = listOf(TajikistanExportLabelItemPayload("Книги", 2))
)

internal fun printableLabel(): TajikistanExportLabel =
    payload().toPrintableLabel()
