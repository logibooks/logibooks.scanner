// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import consulting.sw.logiscanner.net.*
import consulting.sw.logiscanner.printer.ParcelSticker
import org.junit.Assert.assertArrayEquals
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

    @Test fun monitorOffersAutoPrintInBothModes() {
        val tajikistanJob = mutableStateOf(false)
        val printerSelected = mutableStateOf(false)
        val relabelingOn = mutableStateOf(false)
        val kgtAutoPrint = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Row {
                    MonitorRelabelingControls(
                        isTajikistanInvoiceJob = tajikistanJob.value,
                        relabelingOn = relabelingOn.value,
                        relabelingAvailable = true,
                        kgtAutoPrintEnabled = kgtAutoPrint.value,
                        printerSelected = printerSelected.value,
                        onToggleRelabeling = { relabelingOn.value = !relabelingOn.value },
                        onKgtAutoPrintEnabledChange = { kgtAutoPrint.value = it }
                    )
                }
            }
        }

        val relabelingOff = compose.onNodeWithContentDescription("Relabeling is off. Tap to turn on")
        val relabelingEnabled = compose.onNodeWithContentDescription("Relabeling is on. Tap to turn off")
        val kgtPrint = compose.onNodeWithContentDescription("Automatically print sticker")
        val tjPrint = compose.onNodeWithContentDescription("TJ invoice auto-print")

        tjPrint.assertDoesNotExist()
        kgtPrint.assertIsOff().assertIsNotEnabled()
        relabelingOff.assertIsEnabled().performClick()
        relabelingEnabled.assertExists()
        compose.runOnIdle { printerSelected.value = true }
        kgtPrint.assertIsOff().assertIsEnabled().performClick()
        kgtPrint.assertIsOn()
        relabelingEnabled.performClick()
        relabelingOff.assertExists()
        kgtPrint.assertIsOn()

        compose.runOnIdle { tajikistanJob.value = true }
        relabelingOff.assertDoesNotExist()
        kgtPrint.assertDoesNotExist()
        tjPrint.assertIsOff().performClick()
        tjPrint.assertIsOn().performClick()
        tjPrint.assertIsOff()
        assertTrue(kgtAutoPrint.value)
    }

    @Test fun invoicePrintsFromParcelDataWithoutAScan() {
        val payload = "PRINT 1,1\r\n".toByteArray()
        val parcel = mutableStateOf(ScanJobMonitorParcel(
            parcelId = 7, parcelNumber = "PARCEL-7", shk = "SHIP-7",
            sticker = "LABEL-7", productName = "Product description"
        ))
        val printerSelected = mutableStateOf(false)
        val jobId = mutableStateOf<Int?>(1)
        val busy = mutableStateOf(false)
        val requests = mutableListOf<Pair<Int, Int>>()
        val printedInvoices = mutableListOf<Pair<Int, ByteArray>>()
        var printedLabel: ParcelSticker? = null
        compose.setContent {
            MaterialTheme {
                Column {
                    LocalScanResult(
                        lastCode = null, lastParcelCount = null, lastBoxCount = null,
                        lastScanSource = null,
                        lastItemNumbers = emptyList(), lastExtData = null, lastExtId = null,
                        lastTajikistanExportSticker = null, lastScanTime = null,
                        printerSelected = printerSelected.value, printerLoading = busy.value, onPrintKgtSticker = {},
                        snapshot = null, detailSnapshot = null, onPrintBoxSticker = {}
                    )
                    MonitorParcelRow(
                        parcel = parcel.value, registerType = RegisterTypes.WBR_N, weightCorrection = null,
                        expanded = true, highlighted = false, printerSelected = printerSelected.value,
                        printerLoading = busy.value, onPrintKgtSticker = {},
                        onPrintParcelSticker = { printedLabel = it },
                        tajikistanInvoiceJobId = jobId.value,
                        loadTajikistanInvoice = { scanJobId, parcelId ->
                            requests += scanJobId to parcelId
                            check(parcelId == 7) { "Invoice data missing" }
                            payload
                        },
                        onPrintTajikistanInvoice = { scanJobId, label -> printedInvoices += scanJobId to label },
                        onToggleExpanded = {}
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Reprint last sticker").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Print invoice").assertCountEquals(1)
        val printInvoice = compose.onNodeWithContentDescription("Print invoice")
        printInvoice.assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(requests.isEmpty())
            printerSelected.value = true
        }
        printInvoice.assertIsEnabled().performClick()
        assertEquals(listOf(1 to 7), requests)
        assertEquals(1, printedInvoices.single().first)
        assertArrayEquals(payload, printedInvoices.single().second)
        assertEquals(null, printedLabel)
        val buttonBounds = printInvoice.fetchSemanticsNode().boundsInRoot
        val productBounds = compose.onNodeWithText("Product description").fetchSemanticsNode().boundsInRoot
        assertTrue(buttonBounds.center.y >= productBounds.top && buttonBounds.center.y <= productBounds.bottom)
        compose.onNodeWithContentDescription("Print sticker").performClick()
        assertEquals(ParcelSticker.WbrN("LABEL-7"), printedLabel)
        assertEquals(1, printedInvoices.size)

        compose.runOnIdle {
            parcel.value = parcel.value.copy(stickerScanned = true)
        }
        printInvoice.assertIsEnabled()
        compose.runOnIdle { assertEquals(listOf(1 to 7), requests) }

        compose.runOnIdle { busy.value = true }
        printInvoice.assertIsNotEnabled()
        compose.runOnIdle {
            busy.value = false
            parcel.value = parcel.value.copy(parcelId = 8)
        }
        printInvoice.assertIsNotEnabled()
        compose.onNodeWithText("Invoice data missing").assertExists()
        compose.runOnIdle { jobId.value = null }
        printInvoice.assertDoesNotExist()
    }
}
