// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.ui.ScanResultColor
import consulting.sw.logiscanner.ui.determineScanResultColor
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ScanResultColor logic
 */
class ScanResultColorTest {
    
    @Test
    fun scanResultColor_hasAllRequiredColors() {
        // Verify all required colors exist
        val colors = ScanResultColor.entries
        assertTrue(colors.contains(ScanResultColor.NONE))
        assertTrue(colors.contains(ScanResultColor.NOT_FOUND))
        assertTrue(colors.contains(ScanResultColor.OK))
        assertTrue(colors.contains(ScanResultColor.ISSUE))
        assertTrue(colors.contains(ScanResultColor.SERVER_ERROR))
    }
    
    @Test
    fun scanResultColor_determineColor_withHasIssuesTrue_returnsIssue() {
        val item = ScanResultItem(count = 5, extData = null, hasIssues = true)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.ISSUE, color)
    }
    
    @Test
    fun scanResultColor_determineColor_withZeroCount_returnsNotFound() {
        val item = ScanResultItem(count = 0, extData = null, hasIssues = false)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.NOT_FOUND, color)
    }
    
    @Test
    fun scanResultColor_determineColor_withPositiveCount_returnsOk() {
        val item = ScanResultItem(count = 5, extData = null, hasIssues = false)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.OK, color)
    }
    
    @Test
    fun scanResultColor_determineColor_hasIssuesOverridesCount() {
        val item = ScanResultItem(count = 0, extData = null, hasIssues = true)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.ISSUE, color)
    }
    
    @Test
    fun scanResultColor_determineColor_hasIssuesWithPositiveCount_returnsIssue() {
        val item = ScanResultItem(count = 10, extData = null, hasIssues = true)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.ISSUE, color)
    }
}
