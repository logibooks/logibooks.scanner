// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import consulting.sw.logiscanner.net.ScanResultItem
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ScanResultItem DTO
 */
class ScanResultItemTest {
    
    @Test
    fun scanResultItem_defaultHasIssues_isFalse() {
        val item = ScanResultItem(count = 5, extData = null)
        assertEquals(5, item.count)
        assertNull(item.extData)
        assertFalse(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_withHasIssuesTrue_returnsTrue() {
        val item = ScanResultItem(count = 3, extData = "test data", hasIssues = true)
        assertEquals(3, item.count)
        assertEquals("test data", item.extData)
        assertTrue(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_withExtData_returnsCorrectData() {
        val extData = "Extended information for TTS"
        val item = ScanResultItem(count = 10, extData = extData, hasIssues = false)
        assertEquals(10, item.count)
        assertEquals(extData, item.extData)
        assertFalse(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_withNullExtData_handlesCorrectly() {
        val item = ScanResultItem(count = 0, extData = null, hasIssues = true)
        assertEquals(0, item.count)
        assertNull(item.extData)
        assertTrue(item.hasIssues)
    }
    
    @Test
    fun scanResultItem_zeroCount_withNoIssues() {
        val item = ScanResultItem(count = 0, extData = "Zero count", hasIssues = false)
        assertEquals(0, item.count)
        assertEquals("Zero count", item.extData)
        assertFalse(item.hasIssues)
    }
}
