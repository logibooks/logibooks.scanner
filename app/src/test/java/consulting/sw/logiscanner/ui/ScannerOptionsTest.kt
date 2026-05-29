// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerOptionsTest {

    @Test
    fun mainStateDefaultsExternalScannerDisabled() {
        assertFalse(MainState().externalScannerEnabled)
    }

    @Test
    fun scanHintUsesHardwareOnlyTextWhenExternalScannerDisabled() {
        assertEquals(
            R.string.scan_hint_hardware_only,
            scanHintResId(externalScannerEnabled = false)
        )
    }

    @Test
    fun scanHintMentionsExternalScannerWhenEnabled() {
        assertEquals(
            R.string.scan_hint_external_enabled,
            scanHintResId(externalScannerEnabled = true)
        )
    }

    @Test
    fun hidScannerInputFollowsExternalScannerOption() {
        assertFalse(hidScannerInputEnabled(externalScannerEnabled = false))
        assertTrue(hidScannerInputEnabled(externalScannerEnabled = true))
    }

    @Test
    fun hidFocusRecoveryIsSuspendedOnlyForEnabledExternalScannerAndFocusedField() {
        assertFalse(
            hidFocusRecoverySuspended(
                externalScannerEnabled = false,
                textFieldFocused = false
            )
        )
        assertFalse(
            hidFocusRecoverySuspended(
                externalScannerEnabled = false,
                textFieldFocused = true
            )
        )
        assertFalse(
            hidFocusRecoverySuspended(
                externalScannerEnabled = true,
                textFieldFocused = false
            )
        )
        assertTrue(
            hidFocusRecoverySuspended(
                externalScannerEnabled = true,
                textFieldFocused = true
            )
        )
    }
}
