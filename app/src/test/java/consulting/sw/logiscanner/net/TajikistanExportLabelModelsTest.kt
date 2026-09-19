// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TajikistanExportLabelModelsTest {
    private val adapter = Moshi.Builder().build().adapter(ScanResultItem::class.java)

    @Test
    fun parsesFullTajikistanLabelResponse() {
        val result = adapter.fromJson(
            """
            {
              "count": 1,
              "parcelCount": 1,
              "boxCount": 0,
              "scanSource": 10,
              "itemNumbers": ["40856360164"],
              "extData": null,
              "labelTemplate": "TJ_EXPORT",
              "exportLabel": {
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
        assertEquals(LabelTemplates.TAJIKISTAN_EXPORT, result!!.labelTemplate)
        assertEquals("40856360164", result.exportLabel!!.orderNumber)
        assertEquals(2, result.exportLabel.items.single().quantity)
    }

    @Test
    fun acceptsPartialAndLegacyResponses() {
        val partial = adapter.fromJson(
            """{"count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"extData":null,"labelTemplate":"TJ_EXPORT","exportLabel":{"orderNumber":"1"}}"""
        )
        val legacy = adapter.fromJson(
            """{"count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"extData":null}"""
        )

        assertEquals("1", partial!!.exportLabel!!.orderNumber)
        assertNull(partial.exportLabel.accountNumber)
        assertNull(legacy!!.labelTemplate)
        assertNull(legacy.exportLabel)
    }
}
