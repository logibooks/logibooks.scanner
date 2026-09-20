// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
                "accountNumber": "ACC-42",
                "placesCount": 1,
                "dispatchDate": "19.09.26",
                "weightKg": 1.25,
                "declaredValue": 1200.0,
                "currency": "RUB",
                "senderName": "Sender",
                "senderAddress": "Moscow",
                "recipientName": "Recipient",
                "recipientAddress": "Dushanbe",
                "recipientPhone": "+992",
                "items": [{"description":"Books","quantity":2}]
              }
            }
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(StickerTemplates.TAJIKISTAN_EXPORT, result!!.stickerTemplate)
        assertEquals("40856360164", result.exportSticker!!.orderNumber)
        assertEquals(2, result.exportSticker.items.single().quantity)
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
        assertNull(partial.exportSticker.accountNumber)
        assertNull(legacy!!.stickerTemplate)
        assertNull(legacy.exportSticker)
    }
}
