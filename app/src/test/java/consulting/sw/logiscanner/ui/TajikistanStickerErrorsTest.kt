// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.content.Context
import android.content.res.Configuration
import com.squareup.moshi.Moshi
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.TajikistanExportStickerItemPayload
import consulting.sw.logiscanner.net.TajikistanExportStickerPayload
import consulting.sw.logiscanner.printer.payload
import consulting.sw.logiscanner.printer.validateForPrinting
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TajikistanStickerErrorsTest {
    @Test
    fun productionResponseWithoutDispatchDateExplainsMissingConsolidatedInvoiceDate() {
        val result = Moshi.Builder().build().adapter(ScanResultItem::class.java).fromJson(
            """
            {
              "count":1,"parcelCount":1,"boxCount":0,"scanSource":10,"itemNumbers":[],"extData":"",
              "hasIssues":false,"stickerTemplate":"TJ_EXPORT",
              "exportSticker":{
                "orderNumber":"12345678901","dcBankID":"DCB_123","placesCount":1,
                "weightKg":12.060,"costRub":214.00,
                "senderName":"Sender","senderAddress":"Address",
                "recipientName":"Recipient","recipientAddress":"Address","recipientPhone":"123",
                "items":[{"productName":"Goods","quantity":1}]
              }
            }
            """.trimIndent()
        )!!
        val validation = result.exportSticker.validateForPrinting()
        assertNull(validation.sticker)
        assertEquals(
            "Не удалось напечатать стикер. Не указаны данные: дата общей накладной.",
            tajikistanStickerDataError(context("ru"), validation.issues)
        )
        assertEquals(
            "Unable to print sticker. Missing data: consolidated invoice date.",
            tajikistanStickerDataError(context("en"), validation.issues)
        )
    }

    @Test
    fun missingFieldsUseApprovedShipmentAndPartyNames() {
        val issues = payload().copy(orderNumber = " ", senderName = null).validateForPrinting().issues
        assertEquals(
            "Не удалось напечатать стикер. Не указаны данные: номер отправления, грузоотправитель.",
            tajikistanStickerDataError(context("ru"), issues)
        )
    }

    @Test
    fun missingAndInvalidDataAreReportedTogether() {
        val issues = payload().copy(dispatchDate = null, weightKg = 0.0, costRub = -1.0).validateForPrinting().issues
        assertEquals(
            "Не удалось напечатать стикер. Не указаны данные: дата общей накладной. " +
                "Некорректные значения: вес груза, стоимость товаров. Укажите числа больше нуля.",
            tajikistanStickerDataError(context("ru"), issues)
        )
    }

    @Test
    fun singleItemOmitsNumberForBothNameAndQuantity() {
        val issues = payload().copy(items = listOf(TajikistanExportStickerItemPayload(null, 0))).validateForPrinting().issues
        val message = tajikistanStickerDataError(context("ru"), issues)
        assertEquals(
            "Не удалось напечатать стикер. Не указаны данные: наименование товара. " +
                "Некорректные значения: количество товара. Укажите числа больше нуля.",
            message
        )
        assertFalse(message.contains("№"))
    }

    @Test
    fun multipleItemsIdentifyBothFieldsByParcelItemNumber() {
        val issues = payload().copy(
            items = listOf(payload().items.single(), TajikistanExportStickerItemPayload(null, null))
        ).validateForPrinting().issues
        assertEquals(
            "Не удалось напечатать стикер. Не указаны данные: наименование товара №2, количество товара №2.",
            tajikistanStickerDataError(context("ru"), issues)
        )
        assertEquals(
            "Unable to print sticker. Missing data: product name #2, product quantity #2.",
            tajikistanStickerDataError(context("en"), issues)
        )
    }

    @Test
    fun missingPayloadAndNoSavedStickerHaveDifferentMessages() {
        val payload: TajikistanExportStickerPayload? = null
        val context = context("ru")
        assertEquals(
            "Сервер не передал данные для печати стикера",
            tajikistanStickerDataError(context, payload.validateForPrinting().issues)
        )
        assertEquals(
            "Нет стикера для повторной печати. Сначала отсканируйте посылку",
            context.getString(R.string.printer_tj_sticker_no_last)
        )
    }

    private fun context(languageTag: String): Context {
        val application = RuntimeEnvironment.getApplication()
        val configuration = Configuration(application.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return application.createConfigurationContext(configuration)
    }
}
