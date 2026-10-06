// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import consulting.sw.logiscanner.net.TajikistanExportStickerItemPayload
import consulting.sw.logiscanner.net.TajikistanExportStickerPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TajikistanExportStickerValidationTest {

    @Test
    fun validPayloadBuildsPrintableStickerAndTrimsText() {
        val sticker = payload().copy(orderNumber = " 40856360164 ").toPrintableSticker()

        assertNotNull(sticker)
        assertEquals("40856360164", sticker?.orderNumber)
        assertEquals(2, sticker?.items?.single()?.quantity)
    }

    @Test
    fun missingPayloadOrRequiredFieldIsRejected() {
        assertNull(null.toPrintableSticker())
        assertNull(payload().copy(senderAddress = " ").toPrintableSticker())
        assertNull(payload().copy(items = emptyList()).toPrintableSticker())
    }

    @Test
    fun missingOrBlankDcBankIdUsesDefaultValue() {
        assertEquals(DEFAULT_DC_BANK_ID, payload().copy(dcBankID = null).toPrintableSticker()?.dcBankID)
        assertEquals(DEFAULT_DC_BANK_ID, payload().copy(dcBankID = " ").toPrintableSticker()?.dcBankID)
    }

    @Test
    fun invalidNumericOrItemValuesAreRejected() {
        assertNull(payload().copy(placesCount = 0).toPrintableSticker())
        assertNull(payload().copy(weightKg = Double.NaN).toPrintableSticker())
        assertNull(payload().copy(costRub = 0.0).toPrintableSticker())
        assertNull(
            payload().copy(
                items = listOf(TajikistanExportStickerItemPayload(null, 1))
            ).toPrintableSticker()
        )
        assertNull(
            payload().copy(
                items = listOf(TajikistanExportStickerItemPayload("Книги", 0))
            ).toPrintableSticker()
        )
    }

    @Test
    fun validationReportsEveryMissingFieldTogether() {
        val result = TajikistanExportStickerPayload().validateForPrinting()

        assertNull(result.sticker)
        assertEquals(
            listOf(
                TajikistanStickerField.ORDER_NUMBER,
                TajikistanStickerField.PLACES_COUNT,
                TajikistanStickerField.DISPATCH_DATE,
                TajikistanStickerField.WEIGHT,
                TajikistanStickerField.COST,
                TajikistanStickerField.SENDER_NAME,
                TajikistanStickerField.SENDER_ADDRESS,
                TajikistanStickerField.RECIPIENT_ADDRESS,
                TajikistanStickerField.RECIPIENT_PHONE,
                TajikistanStickerField.ITEMS
            ),
            result.issues.map { it.field }
        )
        assertTrue(result.issues.all { it.reason == TajikistanStickerIssueReason.MISSING })
    }

    @Test
    fun validationDistinguishesMissingFromInvalidNumericValues() {
        val missing = payload().copy(placesCount = null, weightKg = null, costRub = null).validateForPrinting()
        assertEquals(3, missing.issues.size)
        assertTrue(missing.issues.all { it.reason == TajikistanStickerIssueReason.MISSING })

        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            val invalid = payload().copy(placesCount = 0, weightKg = value, costRub = value).validateForPrinting()
            assertNull(invalid.sticker)
            assertEquals(3, invalid.issues.size)
            assertTrue(invalid.issues.all { it.reason == TajikistanStickerIssueReason.INVALID })
        }
    }

    @Test
    fun itemNumbersAppearOnlyForMultipleItemsAndPreserveParcelPosition() {
        val badItem = TajikistanExportStickerItemPayload(" ", null)
        val single = payload().copy(items = listOf(badItem)).validateForPrinting()
        assertEquals(2, single.issues.size)
        assertTrue(single.issues.all { it.itemNumber == null })

        val multiple = payload().copy(items = listOf(payload().items.single(), badItem)).validateForPrinting()
        assertEquals(
            listOf(
                TajikistanStickerDataIssue(TajikistanStickerField.PRODUCT_NAME, TajikistanStickerIssueReason.MISSING, 2),
                TajikistanStickerDataIssue(TajikistanStickerField.QUANTITY, TajikistanStickerIssueReason.MISSING, 2)
            ),
            multiple.issues
        )
    }

    @Test
    fun unsupportedCode128OrderNumberIsKeptForNativeTextButNotBarcode() {
        listOf("Заказ", "bad\"value", "ABCDEFGHIJKLMNOP").forEach { orderNumber ->
            val sticker = payload().copy(orderNumber = orderNumber).toPrintableSticker()
            assertNotNull(sticker)
            assertEquals(orderNumber, sticker?.orderNumber)
            assertFalse(isSupportedCode128Value(sticker?.orderNumber.orEmpty()))
        }
    }

    @Test
    fun code128WidthUsesNumericCompactionAndPreservesQuietZones() {
        assertTrue(isSupportedCode128Value("1234567890123456789012345678"))
        assertFalse(isSupportedCode128Value("123456789012345678901234567890"))
    }
}

internal fun payload() = TajikistanExportStickerPayload(
    orderNumber = "40856360164",
    dcBankID = "ACC-42",
    placesCount = 1,
    dispatchDate = "19.09.26",
    weightKg = 1.25,
    costRub = 1200.0,
    senderName = "Sender",
    senderAddress = "123456, Moscow, Tverskaya 1",
    recipientAddress = "Душанбе, Рудаки 1",
    recipientPhone = "+992 900 00 00 00",
    items = listOf(TajikistanExportStickerItemPayload("Книги", 2))
)

internal fun printableSticker(): TajikistanExportSticker =
    requireNotNull(payload().toPrintableSticker())
