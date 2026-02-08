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
        assertTrue(colors.contains(ScanResultColor.YELLOW))
        assertTrue(colors.contains(ScanResultColor.GREEN))
        assertTrue(colors.contains(ScanResultColor.RED))
        assertTrue(colors.contains(ScanResultColor.ORANGE))
    }
    
    @Test
    fun scanResultColor_determineColor_withHasIssuesTrue_returnsOrange() {
        val item = ScanResultItem(count = 5, extData = null, hasIssues = true)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.RED, color)
    }
    
    @Test
    fun scanResultColor_determineColor_withZeroCount_returnsYellow() {
        val item = ScanResultItem(count = 0, extData = null, hasIssues = false)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.YELLOW, color)
    }
    
    @Test
    fun scanResultColor_determineColor_withPositiveCount_returnsGreen() {
        val item = ScanResultItem(count = 5, extData = null, hasIssues = false)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.GREEN, color)
    }
    
    @Test
    fun scanResultColor_determineColor_hasIssuesOverridesCount() {
        val item = ScanResultItem(count = 0, extData = null, hasIssues = true)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.RED, color)
    }
    
    @Test
    fun scanResultColor_determineColor_hasIssuesWithPositiveCount_returnsOrange() {
        val item = ScanResultItem(count = 10, extData = null, hasIssues = true)

        val color = determineScanResultColor(item)

        assertEquals(ScanResultColor.RED, color)
    }
}
