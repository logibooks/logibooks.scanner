// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ScanResultItem DTO
 */
class ScanResultItemTest {
    
    @Test
    fun scanResultItem_defaultHasIssues_isFalse() {
        val item = scanResultItem(count = 5, extData = null)
        assertEquals(5, item.count)
        assertNull(item.extData)
        assertNull(item.extId)
        assertFalse(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_withHasIssuesTrue_returnsTrue() {
        val item = scanResultItem(count = 3, extData = "test data", hasIssues = true)
        assertEquals(3, item.count)
        assertEquals("test data", item.extData)
        assertTrue(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_withExtData_returnsCorrectData() {
        val extData = "Extended information for TTS"
        val item = scanResultItem(count = 10, extData = extData, hasIssues = false)
        assertEquals(10, item.count)
        assertEquals(extData, item.extData)
        assertFalse(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_withNullExtData_handlesCorrectly() {
        val item = scanResultItem(count = 0, extData = null, hasIssues = true)
        assertEquals(0, item.count)
        assertNull(item.extData)
        assertTrue(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_zeroCount_withNoIssues() {
        val item = scanResultItem(count = 0, extData = "Zero count", hasIssues = false)
        assertEquals(0, item.count)
        assertEquals("Zero count", item.extData)
        assertFalse(item.hasIssues)
    }

    @Test
    fun scanResultItem_contractFields_returnScanClassification() {
        val item = scanResultItem(
            count = 2,
            parcelCount = 2,
            boxCount = 1,
            scanSource = ScannedItemSources.BOX_STICKER,
            itemNumbers = listOf("BOX-1"),
            extData = "box"
        )

        assertEquals(2, item.parcelCount)
        assertEquals(1, item.boxCount)
        assertEquals(ScannedItemSources.BOX_STICKER, item.scanSource)
        assertEquals(listOf("BOX-1"), item.itemNumbers)
    }

    @Test
    fun scanResultItem_withExtId_returnsKgtNumber() {
        val item = scanResultItem(count = 1, extData = null, extId = "15")

        assertEquals("15", item.extId)
    }

    private fun scanResultItem(
        count: Int,
        parcelCount: Int = count,
        boxCount: Int = 0,
        scanSource: Int = ScannedItemSources.PARCEL_STICKER,
        itemNumbers: List<String> = emptyList(),
        extData: String?,
        extId: String? = null,
        hasIssues: Boolean = false
    ): ScanResultItem {
        return ScanResultItem(
            count = count,
            parcelCount = parcelCount,
            boxCount = boxCount,
            scanSource = scanSource,
            itemNumbers = itemNumbers,
            extData = extData,
            extId = extId,
            hasIssues = hasIssues
        )
    }
}
