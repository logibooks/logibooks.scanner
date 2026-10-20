// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.runtime.mutableStateOf
import consulting.sw.logiscanner.net.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class WbrBoxStickerRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manuallyEnteredCodeIsDisplayedOnceEvenWithoutScanAndWithoutWbrPrinting() {
        val box = mutableStateOf(ScanJobMonitorBox(boxId = 7, boxSticker = "manual", boxStickerScanned = false))
        compose.setContent { MaterialTheme { BoxStickerAttribute(box.value) } }
        compose.onAllNodesWithText("Sticker code").assertCountEquals(1)
        compose.onAllNodesWithText("manual").assertCountEquals(1)
        compose.onNodeWithText("Scanned sticker").assertDoesNotExist()
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.runOnIdle {
            box.value = box.value.copy(boxSticker = "$1:1:4542503776:362877", printableTemplates = listOf("WBR_BOX"))
        }
        compose.onNodeWithText("manual").assertDoesNotExist()
        compose.onAllNodesWithText("$1:1:4542503776:362877").assertCountEquals(1)
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
    }

    @Test fun savedStickerRendersAndPrintsFromBoxNumberRowOnlyWhenAvailable() {
        val snapshot = ScanJobMonitorSnapshot(companyId = RegisterTypes.WBR)
        val box = mutableStateOf(ScanJobMonitorBox(boxId = 7, boxCode = "4542503776", boxSticker = "$1:1:4542503776:362877", printableTemplates = listOf("WBR_BOX")))
        val busy = mutableStateOf(false)
        val printerSelected = mutableStateOf(true)
        var printed: Int? = null
        compose.setContent {
            MaterialTheme {
                MonitorBoxDetail(
                    snapshot = snapshot.copy(box = box.value), loading = false, highlightedParcelId = null,
                    printerSelected = printerSelected.value, printerLoading = busy.value,
                    onPrintKgtSticker = {}, onPrintParcelSticker = {}, onOpenRegister = {},
                    lastTajikistanExportSticker = null, canPrintTajikistanExportSticker = false,
                    onPrintTajikistanExportSticker = {},
                    onPrintBoxSticker = { printed = it }
                )
            }
        }
        compose.onNodeWithText("Sticker code").assertIsDisplayed()
        compose.onNodeWithText("$1:1:4542503776:362877").assertIsDisplayed()
        compose.onNodeWithContentDescription("Print box sticker").assertIsEnabled().performClick()
        assertEquals(7, printed)
        val buttonBounds = compose.onNodeWithContentDescription("Print box sticker").fetchSemanticsNode().boundsInRoot
        val titleBounds = compose.onNodeWithText("Box 4542503776").fetchSemanticsNode().boundsInRoot
        val stickerBounds = compose.onNodeWithText("Sticker").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val numberBounds = compose.onNodeWithText("4542503776").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val codeBounds = compose.onNodeWithText("Sticker code").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(buttonBounds.top > titleBounds.bottom)
        org.junit.Assert.assertTrue(buttonBounds.center.y >= stickerBounds.top && buttonBounds.center.y <= stickerBounds.bottom)
        org.junit.Assert.assertTrue(buttonBounds.center.y >= numberBounds.top && buttonBounds.center.y <= numberBounds.bottom)
        org.junit.Assert.assertTrue(stickerBounds.bottom <= codeBounds.top)
        compose.runOnIdle { printerSelected.value = false }
        compose.onNodeWithContentDescription("Print box sticker").assertIsNotEnabled()
        compose.runOnIdle { printerSelected.value = true }
        compose.runOnIdle { busy.value = true }
        compose.onNodeWithContentDescription("Print box sticker").assertIsNotEnabled()
        compose.runOnIdle { box.value = box.value.copy(boxSticker = null, printableTemplates = emptyList()) }
        compose.onAllNodesWithText("—").assertCountEquals(3)
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.onNodeWithText("Sticker").assertIsDisplayed()
        compose.onNodeWithText("4542503776").assertIsDisplayed()
        compose.runOnIdle { box.value = box.value.copy(area = ScanJobMonitorAreas.UNASSIGNED) }
        compose.onNodeWithText("—").assertDoesNotExist()
        compose.onNodeWithText("Sticker").assertDoesNotExist()
    }

    @Test fun latestNonBoxResultsPreserveScannedCodeWithoutItemNumbersOrSnapshot() {
        val source = mutableStateOf<Int?>(ScannedItemSources.PARCEL_STICKER)
        val code = mutableStateOf<String?>("raw-scanned-sticker")
        val numbers = mutableStateOf(listOf("PARCEL-7"))
        val snapshot = mutableStateOf<ScanJobMonitorSnapshot?>(ScanJobMonitorSnapshot(companyId = RegisterTypes.WBR))
        compose.setContent {
            MaterialTheme {
                LocalScanResult(
                    lastCode = code.value, lastParcelCount = 1, lastBoxCount = 0,
                    lastScanSource = source.value, lastItemNumbers = numbers.value,
                    lastExtData = null, lastExtId = null, lastTajikistanExportSticker = null,
                    lastScanTime = null, printerSelected = false, printerLoading = false,
                    onPrintKgtSticker = {}, snapshot = snapshot.value, detailSnapshot = null,
                    onPrintBoxSticker = {}
                )
            }
        }
        compose.onNodeWithText("PARCEL-7").assertIsDisplayed()
        compose.onAllNodesWithText("raw-scanned-sticker").assertCountEquals(1)
        compose.onNodeWithText("Sticker").assertIsDisplayed()
        for (scanSource in listOf(ScannedItemSources.NOT_IN_REGISTER, null, -1)) {
            compose.runOnIdle { source.value = scanSource; numbers.value = emptyList() }
            compose.onNodeWithText("PARCEL-7").assertDoesNotExist()
            compose.onNodeWithText("raw-scanned-sticker").assertIsDisplayed()
            compose.onNodeWithText("Sticker").assertIsDisplayed()
        }
        compose.runOnIdle { snapshot.value = null }
        compose.onNodeWithText("raw-scanned-sticker").assertIsDisplayed()
        for (emptyCode in listOf(null, "", "   ")) {
            compose.runOnIdle { code.value = emptyCode }
            compose.onNodeWithText("raw-scanned-sticker").assertDoesNotExist()
            compose.onNodeWithText("Sticker").assertDoesNotExist()
        }
    }

    @Test fun latestBoxPrintUsesScannedNumberAndCommonAvailabilityRules() {
        val scannedBox = ScanJobMonitorBox(boxId = 7, boxCode = "BOX-7", printableTemplates = listOf("WBR_BOX"))
        val otherBox = scannedBox.copy(boxId = 8, boxCode = "BOX-8")
        val snapshot = mutableStateOf(ScanJobMonitorSnapshot(scanJobId = 1, companyId = RegisterTypes.WBR, boxes = listOf(scannedBox, otherBox)))
        val detail = mutableStateOf(snapshot.value.copy(box = otherBox))
        val printerSelected = mutableStateOf(true)
        val busy = mutableStateOf(false)
        val numbers = mutableStateOf(listOf("BOX-7"))
        val source = mutableStateOf(ScannedItemSources.BOX_STICKER)
        var printed: Int? = null
        compose.setContent {
            MaterialTheme {
                LocalScanResult(
                    lastCode = "raw-scanned-sticker", lastParcelCount = 1, lastBoxCount = 1,
                    lastScanSource = source.value, lastItemNumbers = numbers.value,
                    lastExtData = null, lastExtId = null, lastTajikistanExportSticker = null,
                    lastScanTime = null, printerSelected = printerSelected.value, printerLoading = busy.value,
                    onPrintKgtSticker = {},
                    snapshot = snapshot.value, detailSnapshot = detail.value, onPrintBoxSticker = { printed = it }
                )
            }
        }
        compose.onNodeWithText("raw-scanned-sticker").assertDoesNotExist()
        compose.onNodeWithText("Sticker").assertDoesNotExist()
        compose.onNodeWithText("BOX-7").assertIsDisplayed()
        compose.onNodeWithContentDescription("Print box sticker").assertIsEnabled().performClick()
        assertEquals(7, printed)
        compose.runOnIdle { printerSelected.value = false }
        compose.onNodeWithContentDescription("Print box sticker").assertIsNotEnabled()
        compose.runOnIdle { printerSelected.value = true; busy.value = true }
        compose.onNodeWithContentDescription("Print box sticker").assertIsNotEnabled()
        compose.runOnIdle { busy.value = false; detail.value = snapshot.value.copy(box = scannedBox.copy(printableTemplates = emptyList())) }
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.onNodeWithText("raw-scanned-sticker").assertDoesNotExist()
        compose.runOnIdle { detail.value = snapshot.value.copy(box = scannedBox) }
        compose.onNodeWithContentDescription("Print box sticker").assertIsEnabled()
        compose.runOnIdle { snapshot.value = snapshot.value.copy(companyId = RegisterTypes.OZON) }
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.onNodeWithText("raw-scanned-sticker").assertIsDisplayed()
        compose.runOnIdle { snapshot.value = snapshot.value.copy(companyId = RegisterTypes.WBR); numbers.value = listOf("UNKNOWN") }
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.runOnIdle { numbers.value = listOf("BOX-7", "BOX-8") }
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.runOnIdle { numbers.value = listOf("BOX-7"); source.value = ScannedItemSources.PARCEL_STICKER }
        compose.onNodeWithContentDescription("Print box sticker").assertDoesNotExist()
        compose.onNodeWithText("raw-scanned-sticker").assertIsDisplayed()
    }
}
