// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.LookupScanCandidate
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ru-w400dp-h800dp")
class LookupBoxTargetsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lookupResultRendersBothListsAndTheEmptyState() {
        val box = mutableStateOf(LookupBoxResult(
            outstanding = listOf(LookupScanCandidate(1, "A"), LookupScanCandidate(2, "A"), LookupScanCandidate(3, "B")),
            confirmed = listOf(LookupScanCandidate(4, "A"))
        ))
        compose.setContent {
            MaterialTheme {
                LocalScanResult(
                    lastCode = "LABEL",
                    lastParcelCount = 0,
                    lastBoxCount = 1,
                    lastScanSource = ScannedItemSources.BOX_STICKER,
                    lastItemNumbers = listOf("017"),
                    lastExtData = null,
                    lastLookupBox = box.value,
                    lastExtId = null,
                    lastTajikistanExportSticker = null,
                    lastScanTime = null,
                    printerSelected = false,
                    printerLoading = false,
                    onPrintKgtSticker = {},
                    snapshot = null,
                    detailSnapshot = null,
                    onPrintMonitorSticker = { _, _ -> }
                )
            }
        }
        compose.onNodeWithText("Посылки для поиска").assertIsDisplayed()
        compose.onNodeWithText("A (2 товара), B").assertIsDisplayed()
        compose.onNodeWithText("Найдены ранее:").assertIsDisplayed()
        compose.onNodeWithText("A").assertIsDisplayed()

        compose.runOnIdle { box.value = LookupBoxResult(emptyList(), emptyList()) }
        compose.onNodeWithText("Нет посылок для поиска").assertIsDisplayed()
    }

    @Test fun boxColorReflectsBothTargetListsAndListsGroupIndependently() {
        val result = ScanResultItem(
            count = 0, parcelCount = 0, boxCount = 1, scanSource = ScannedItemSources.BOX_STICKER,
            itemNumbers = listOf("017"), extData = null, lookupOutcome = "inspectBox",
            lookupCandidates = listOf(LookupScanCandidate(1, "A"), LookupScanCandidate(2, "A"), LookupScanCandidate(3, "B")),
            confirmedLookupCandidates = listOf(LookupScanCandidate(4, "A"))
        )
        assertEquals(ScanResultColor.OK, determineScanResultColor(result))
        assertEquals(ScanResultColor.OK, determineScanResultColor(result.copy(confirmedLookupCandidates = emptyList())))

        val noOutstanding = result.copy(lookupOutcome = "noOutstanding", lookupCandidates = emptyList())
        assertEquals(ScanResultColor.OK, determineScanResultColor(noOutstanding))
        assertEquals(
            ScanResultColor.NOT_FOUND,
            determineScanResultColor(noOutstanding.copy(confirmedLookupCandidates = emptyList()))
        )

        val display = result.lookupBoxResult()!!
        assertEquals(mapOf("A" to 2, "B" to 1), display.outstanding.countsByParcelNumber())
        assertEquals(mapOf("A" to 1), display.confirmed.countsByParcelNumber())
    }

    @Test fun RussianGoodsCountUsesTheCorrectPlural() {
        val resources = RuntimeEnvironment.getApplication().resources
        for ((count, noun) in mapOf(1 to "товар", 2 to "товара", 5 to "товаров", 11 to "товаров",
            12 to "товаров", 14 to "товаров", 21 to "товар", 22 to "товара")) {
            assertEquals("$count $noun", resources.getQuantityString(R.plurals.monitor_lookup_goods_count, count, count))
        }
    }
}
