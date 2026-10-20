// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import consulting.sw.logiscanner.net.*
import consulting.sw.logiscanner.printer.ParcelSticker
import consulting.sw.logiscanner.printer.TajikistanExportSticker
import consulting.sw.logiscanner.printer.TajikistanExportStickerItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class InvoicePrintingRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invoicePrintsBesideMatchingProductAndPreservesEligibilityAndParcelLabel() {
        val invoice = TajikistanExportSticker(
            orderNumber = "SHIP-7", dcBankID = "DCB", placesCount = 1,
            dispatchDate = "22.10.2026", weightKg = 1.0, costRub = 100.0,
            senderName = "Sender", senderAddress = "Sender address",
            recipientAddress = "Recipient address", recipientPhone = "123456789",
            items = listOf(TajikistanExportStickerItem("Product description", 1))
        )
        val parcel = mutableStateOf(ScanJobMonitorParcel(
            parcelId = 7, parcelNumber = "PARCEL-7", shk = "SHIP-7",
            sticker = "LABEL-7", productName = "Product description"
        ))
        val printerSelected = mutableStateOf(true)
        val job = ScanJob(
            id = 1, name = "Job", description = null, status = "InProgress", type = "Scan",
            registerType = RegisterTypes.WBR_N, stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )
        val busy = mutableStateOf(false)
        val registerType = mutableStateOf(RegisterTypes.WBR_N)
        var invoicePrints = 0
        var printedLabel: ParcelSticker? = null
        compose.setContent {
            MaterialTheme {
                Column {
                    LocalScanResult(
                        lastCode = "LABEL-7", lastParcelCount = 1, lastBoxCount = 0,
                        lastScanSource = ScannedItemSources.PARCEL_STICKER,
                        lastItemNumbers = listOf("PARCEL-7"), lastExtData = null, lastExtId = null,
                        lastTajikistanExportSticker = invoice, lastScanTime = null,
                        printerSelected = printerSelected.value, printerLoading = busy.value, onPrintKgtSticker = {},
                        snapshot = null, detailSnapshot = null, onPrintBoxSticker = {}
                    )
                    MonitorParcelRow(
                        parcel = parcel.value, registerType = registerType.value, weightCorrection = null,
                        expanded = true, highlighted = false, printerSelected = printerSelected.value,
                        printerLoading = busy.value, onPrintKgtSticker = {},
                        onPrintParcelSticker = { printedLabel = it },
                        lastTajikistanExportSticker = invoice,
                        canPrintTajikistanExportSticker = canRepeatTajikistanExportSticker(
                            printerSelected = printerSelected.value, job = job, hasSticker = true
                        ),
                        onPrintTajikistanExportSticker = { invoicePrints++ }, onToggleExpanded = {}
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Reprint last sticker").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Print invoice").assertCountEquals(1)
        val printInvoice = compose.onNodeWithContentDescription("Print invoice")
        printInvoice.assertIsEnabled().performClick()
        assertEquals(1, invoicePrints)
        assertEquals(null, printedLabel)
        val buttonBounds = printInvoice.fetchSemanticsNode().boundsInRoot
        val productBounds = compose.onNodeWithText("Product description").fetchSemanticsNode().boundsInRoot
        assertTrue(buttonBounds.center.y >= productBounds.top && buttonBounds.center.y <= productBounds.bottom)
        compose.onNodeWithContentDescription("Print sticker").performClick()
        assertEquals(ParcelSticker.WbrN("LABEL-7"), printedLabel)
        assertEquals(1, invoicePrints)

        compose.runOnIdle { printerSelected.value = false }
        printInvoice.assertIsNotEnabled()
        compose.runOnIdle { printerSelected.value = true; busy.value = true }
        printInvoice.assertIsNotEnabled()
        compose.runOnIdle { busy.value = false; parcel.value = parcel.value.copy(shk = "OTHER") }
        printInvoice.assertDoesNotExist()
        compose.runOnIdle { parcel.value = parcel.value.copy(shk = "SHIP-7"); registerType.value = RegisterTypes.OZON }
        printInvoice.assertDoesNotExist()
        compose.runOnIdle { registerType.value = RegisterTypes.WBR_N }
        printInvoice.assertIsEnabled()
    }
}
