// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w400dp-h800dp")
class ScanJobMonitorRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun overviewShowsOnlySuppliedLookupCountsIncludingZero() {
        val snapshot = mutableStateOf(ScanJobMonitorSnapshot(
            totalBoxes = 30,
            boxesWithStickerScanned = 10,
            boxesWithStickerNotScanned = 20,
            totalParcels = 70,
            parcelsWithStickerScanned = 30,
            parcelsWithStickerNotScanned = 40,
            restrictedParcels = 11,
            scannedItemsNotInRegister = 12,
            lookupRemainingCount = 7
        ))
        compose.setContent {
            MaterialTheme {
                ScanJobMonitorPanel(
                    snapshot = snapshot.value,
                    detailSnapshot = null,
                    selectedScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES),
                    lastCode = null,
                    lastParcelCount = null,
                    lastBoxCount = null,
                    lastScanSource = null,
                    lastItemNumbers = emptyList(),
                    lastExtData = null,
                    lastExtId = null,
                    lastTajikistanExportSticker = null,
                    tajikistanInvoiceJobId = null,
                    lastScanTime = null,
                    loading = false,
                    detailLoading = false,
                    error = null,
                    autoFollow = false,
                    bulkyItemsMode = BulkyItemsModes.OFF,
                    kgtAutoPrintEnabled = false,
                    relabelingModeAvailable = false,
                    printerSelected = false,
                    printerLoading = false,
                    printerMessage = null,
                    printerError = null,
                    onPrintKgtSticker = {},
                    onPrintParcelSticker = {},
                    loadTajikistanInvoice = { _, _ -> error("Not a TJ job") },
                    onPrintTajikistanInvoice = { _, _ -> },
                    jumpNumber = "",
                    jumpLoading = false,
                    highlightedParcelId = null,
                    onOpenRegister = {},
                    onOpenBox = {},
                    onToggleAutoFollow = {},
                    onToggleBulkyItemsMode = {},
                    onKgtAutoPrintEnabledChange = {},
                    onJumpNumberChange = {},
                    onJumpToNumber = {},
                    onPrintBoxSticker = {}
                )
            }
        }

        assertCount("Remaining to find", "7")
        compose.onNodeWithText("Found").assertDoesNotExist()
        compose.onNodeWithText("Not found").assertDoesNotExist()

        compose.runOnIdle {
            snapshot.value = snapshot.value.copy(lookupRemainingCount = 2, lookupFoundCount = 3, lookupNotFoundCount = 0)
        }
        assertCount("Found", "3")
        assertCount("Not found", "0")
        assertCount("Remaining to find", "2")

        compose.runOnIdle {
            snapshot.value = snapshot.value.copy(lookupRemainingCount = 0, lookupFoundCount = null, lookupNotFoundCount = 4)
        }
        assertCount("Remaining to find", "0")
        assertCount("Not found", "4")
        compose.onNodeWithText("Found").assertDoesNotExist()

        compose.runOnIdle { snapshot.value = ScanJobMonitorSnapshot() }
        compose.onNodeWithText("Remaining to find").assertDoesNotExist()
        compose.onNodeWithText("Found").assertDoesNotExist()
        compose.onNodeWithText("Not found").assertDoesNotExist()
    }

    private fun assertCount(label: String, value: String) {
        compose.onNodeWithText(label).assertIsDisplayed()
        compose.onNodeWithText(value).assertIsDisplayed()
    }
}
