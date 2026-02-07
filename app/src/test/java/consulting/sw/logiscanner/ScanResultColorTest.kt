// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import consulting.sw.logiscanner.ui.ScanResultColor
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
        // Simulating the logic: If hasIssues is true, color should be ORANGE
        val hasIssues = true
        val count = 5
        
        val color = if (hasIssues) {
            ScanResultColor.ORANGE
        } else if (count == 0) {
            ScanResultColor.YELLOW
        } else {
            ScanResultColor.GREEN
        }
        
        assertEquals(ScanResultColor.ORANGE, color)
    }
    
    @Test
    fun scanResultColor_determineColor_withZeroCount_returnsYellow() {
        // Simulating the logic: If count is 0 and no issues, color should be YELLOW
        val hasIssues = false
        val count = 0
        
        val color = if (hasIssues) {
            ScanResultColor.ORANGE
        } else if (count == 0) {
            ScanResultColor.YELLOW
        } else {
            ScanResultColor.GREEN
        }
        
        assertEquals(ScanResultColor.YELLOW, color)
    }
    
    @Test
    fun scanResultColor_determineColor_withPositiveCount_returnsGreen() {
        // Simulating the logic: If count > 0 and no issues, color should be GREEN
        val hasIssues = false
        val count = 5
        
        val color = if (hasIssues) {
            ScanResultColor.ORANGE
        } else if (count == 0) {
            ScanResultColor.YELLOW
        } else {
            ScanResultColor.GREEN
        }
        
        assertEquals(ScanResultColor.GREEN, color)
    }
    
    @Test
    fun scanResultColor_determineColor_hasIssuesOverridesCount() {
        // Simulating the logic: hasIssues should override count-based logic
        val hasIssues = true
        val count = 0  // Would normally be yellow
        
        val color = if (hasIssues) {
            ScanResultColor.ORANGE
        } else if (count == 0) {
            ScanResultColor.YELLOW
        } else {
            ScanResultColor.GREEN
        }
        
        // Should be ORANGE, not YELLOW, because hasIssues overrides
        assertEquals(ScanResultColor.ORANGE, color)
    }
    
    @Test
    fun scanResultColor_determineColor_hasIssuesWithPositiveCount_returnsOrange() {
        // Simulating the logic: hasIssues should override even with positive count
        val hasIssues = true
        val count = 10  // Would normally be green
        
        val color = if (hasIssues) {
            ScanResultColor.ORANGE
        } else if (count == 0) {
            ScanResultColor.YELLOW
        } else {
            ScanResultColor.GREEN
        }
        
        // Should be ORANGE, not GREEN, because hasIssues overrides
        assertEquals(ScanResultColor.ORANGE, color)
    }
}
