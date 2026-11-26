// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.LookupScanCandidate
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ru")
class LookupBoxTargetsTest {
    @Test fun bothBoxOutcomesAreGreenAndListsGroupIndependently() {
        val result = ScanResultItem(
            count = 0, parcelCount = 0, boxCount = 1, scanSource = ScannedItemSources.BOX_STICKER,
            itemNumbers = listOf("017"), extData = null, lookupOutcome = "inspectBox",
            lookupCandidates = listOf(LookupScanCandidate(1, "A"), LookupScanCandidate(2, "A"), LookupScanCandidate(3, "B")),
            confirmedLookupCandidates = listOf(LookupScanCandidate(4, "A"))
        )
        assertEquals(ScanResultColor.OK, determineScanResultColor(result))
        assertEquals(ScanResultColor.OK, determineScanResultColor(result.copy(lookupOutcome = "noOutstanding")))
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
