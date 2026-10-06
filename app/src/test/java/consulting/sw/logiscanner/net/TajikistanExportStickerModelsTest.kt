// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import com.squareup.moshi.Moshi
import consulting.sw.logiscanner.printer.TajikistanExportStickerRenderer
import consulting.sw.logiscanner.printer.payload
import consulting.sw.logiscanner.printer.validateForPrinting
import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TajikistanExportStickerModelsTest {
    private val adapter = Moshi.Builder().build().adapter(ScanResultItem::class.java)

    @Test
    fun parsesFullTajikistanStickerResponse() {
        val result = adapter.fromJson(
            """
            {
              "count": 1,
              "parcelCount": 1,
              "boxCount": 0,
              "scanSource": 10,
              "itemNumbers": ["40856360164"],
              "extData": null,
              "stickerTemplate": "TJ_EXPORT",
              "exportSticker": {
                "orderNumber": "40856360164",
                "dcBankID": "ACC-42",
                "placesCount": 1,
                "dispatchDate": "19.09.26",
                "weightKg": 1.25,
                "costRub": 1200.0,
                "senderName": "Sender",
                "senderAddress": "Moscow",
                "recipientName": "Recipient",
                "recipientAddress": "Dushanbe",
                "recipientPhone": "+992",
                "items": [{"productName":"Books","quantity":2}]
              }
            }
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(StickerTemplates.TAJIKISTAN_EXPORT, result!!.stickerTemplate)
        assertEquals("40856360164", result.exportSticker!!.orderNumber)
        assertEquals("ACC-42", result.exportSticker.dcBankID)
        assertEquals(1200.0, result.exportSticker.costRub)
        assertEquals("Books", result.exportSticker.items.single().productName)
        assertEquals(2, result.exportSticker.items.single().quantity)
    }

    @Test
    fun recipientNamesInOlderResponsesAreIgnoredAndMissingNamesPrintSuccessfully() {
        val stickerAdapter = Moshi.Builder().build().adapter(TajikistanExportStickerPayload::class.java)
        val currentJson = stickerAdapter.toJson(payload())
        val olderJson = currentJson.replaceFirst("{", "{\"recipientName\":\"Иванов Иван Иванович\",")
        assertFalse(currentJson.contains("recipientName"))
        val current = stickerAdapter.fromJson(currentJson).validateForPrinting()
        val older = stickerAdapter.fromJson(olderJson).validateForPrinting()

        assertEquals(emptyList<Any>(), current.issues)
        assertEquals(current, older)
        val tspl = TajikistanExportStickerRenderer().render(requireNotNull(older.sticker))
            .toString(Charset.forName("windows-1251"))
        assertFalse(tspl.contains("Иванов"))
        assertFalse(tspl.contains("TEXT 16,212,"))
    }

    @Test
    fun parsesAndPrintsRubleParcelTotalWithoutMultiplyingByQuantity() {
        val stickerAdapter = Moshi.Builder().build().adapter(TajikistanExportStickerPayload::class.java)
        val json = stickerAdapter.toJson(payload().copy(costRub = 1234.50))
        val validation = stickerAdapter.fromJson(json).validateForPrinting()
        val sticker = requireNotNull(validation.sticker)
        val tspl = TajikistanExportStickerRenderer().render(sticker).toString(Charset.forName("windows-1251"))

        assertEquals(2, sticker.items.single().quantity)
        assertEquals(1234.50, sticker.costRub)
        assertTrue(tspl.contains("TEXT 280,118,\"1\",0,1,1,\"1234,50 руб.\""))
        assertFalse(tspl.contains("2469,00 руб."))
        assertFalse(tspl.contains("TEXT 16,212,"))
    }

    @Test
    fun acceptsPartialAndPreStickerContractResponses() {
        val partial = adapter.fromJson(
            """{"count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"extData":null,"stickerTemplate":"TJ_EXPORT","exportSticker":{"orderNumber":"1"}}"""
        )
        val legacy = adapter.fromJson(
            """{"count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"extData":null,"labelTemplate":"TJ_EXPORT","exportLabel":{"orderNumber":"legacy"}}"""
        )

        assertEquals("1", partial!!.exportSticker!!.orderNumber)
        assertNull(partial.exportSticker.dcBankID)
        assertNull(legacy!!.stickerTemplate)
        assertNull(legacy.exportSticker)
    }
}
