// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify BuildConfig values are properly set for different build variants.
 */
class BuildConfigTest {
    @Test
    fun serverUrl_isNotEmpty() {
        assertNotNull(BuildConfig.SERVER_URL)
        assertTrue(BuildConfig.SERVER_URL.isNotEmpty())
    }

    @Test
    fun serverUrl_hasValidFormat() {
        assertTrue(
            BuildConfig.SERVER_URL.startsWith("http://") ||
            BuildConfig.SERVER_URL.startsWith("https://")
        )
        assertTrue(BuildConfig.SERVER_URL.endsWith("/"))
    }

    @Test
    fun isDebug_matchesBuildType() {
        // In debug builds, IS_DEBUG should be true
        // In release builds, IS_DEBUG should be false
        // This test will pass in both variants
        assertEquals(BuildConfig.DEBUG, BuildConfig.IS_DEBUG)
    }
}
