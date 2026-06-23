// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerOptionsTest {

    @Test
    fun mainStateDefaultsExternalScannerDisabled() {
        assertFalse(MainState().externalScannerEnabled)
    }

    @Test
    fun mainStateDefaultsSettingsClosed() {
        assertFalse(MainState().settingsOpen)
    }

    @Test
    fun settingsOpenClosePreservesScanState() {
        val state = MainState(
            externalScannerEnabled = true,
            isLoggedIn = true,
            selectedScanJob = scanJob(registerType = RegisterTypes.WBR),
            selectedScanJobTypeDisplay = "WBR",
            bulkyItemsMode = BulkyItemsModes.NOTIFY,
            printerAutoPrintEnabled = true,
            printerBluetoothAddress = "AA:BB:CC:DD:EE:FF",
            isScanning = true,
            lastCode = "123"
        )

        val opened = openSettingsState(state)
        val closed = closeSettingsState(opened)

        assertEquals(state.copy(settingsOpen = true), opened)
        assertEquals(state, closed)
    }

    @Test
    fun mainStateDefaultsBulkyItemsModeOff() {
        assertEquals(BulkyItemsModes.OFF, MainState().bulkyItemsMode)
    }

    @Test
    fun mainStateDefaultsPrinterAutoPrintDisabled() {
        assertFalse(MainState().printerAutoPrintEnabled)
    }

    @Test
    fun mainStateDefaultsPrinterAddressEmpty() {
        assertNull(MainState().printerBluetoothAddress)
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

    @Test
    fun bulkyItemsModeIsEnabledOnlyForWbrScanJobs() {
        assertTrue(bulkyItemsModeEnabled(scanJob(registerType = RegisterTypes.WBR)))
        assertFalse(bulkyItemsModeEnabled(scanJob(registerType = 1)))
        assertFalse(bulkyItemsModeEnabled(null))
    }

    @Test
    fun normalizeBulkyItemsModeRejectsNonWbrAndInvalidValues() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)

        assertEquals(BulkyItemsModes.SILENT, normalizeBulkyItemsMode(wbrJob, BulkyItemsModes.SILENT))
        assertEquals(BulkyItemsModes.NOTIFY, normalizeBulkyItemsMode(wbrJob, BulkyItemsModes.NOTIFY))
        assertEquals(BulkyItemsModes.OFF, normalizeBulkyItemsMode(wbrJob, 99))
        assertEquals(BulkyItemsModes.OFF, normalizeBulkyItemsMode(scanJob(registerType = 1), BulkyItemsModes.NOTIFY))
    }

    @Test
    fun nextBulkyItemsModeCyclesOnlyForWbr() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)

        assertEquals(BulkyItemsModes.SILENT, nextBulkyItemsMode(wbrJob, BulkyItemsModes.OFF))
        assertEquals(BulkyItemsModes.NOTIFY, nextBulkyItemsMode(wbrJob, BulkyItemsModes.SILENT))
        assertEquals(BulkyItemsModes.OFF, nextBulkyItemsMode(wbrJob, BulkyItemsModes.NOTIFY))
        assertEquals(BulkyItemsModes.OFF, nextBulkyItemsMode(scanJob(registerType = 1), BulkyItemsModes.SILENT))
    }

    @Test
    fun kgtLabelCodeTrimsAndRejectsBlankValues() {
        assertEquals("15", kgtLabelCode(" 15 "))
        assertNull(kgtLabelCode(" "))
        assertNull(kgtLabelCode(null))
    }

    @Test
    fun canManualPrintKgtLabelRequiresCode() {
        assertTrue(canManualPrintKgtLabel("15"))
        assertFalse(canManualPrintKgtLabel(" "))
    }

    @Test
    fun shouldAutoPrintKgtLabelRequiresEnabledWbrModeAndGeneratedCode() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val result = scanResultItem(extId = "15")

        assertTrue(
            shouldAutoPrintKgtLabel(
                autoPrintEnabled = true,
                job = wbrJob,
                bulkyItemsMode = BulkyItemsModes.SILENT,
                result = result
            )
        )
        assertFalse(shouldAutoPrintKgtLabel(false, wbrJob, BulkyItemsModes.SILENT, result))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.OFF, result))
        assertFalse(shouldAutoPrintKgtLabel(true, scanJob(registerType = 1), BulkyItemsModes.SILENT, result))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = null)))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = "15", count = 0)))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = "15", hasIssues = true)))
    }

    private fun scanJob(registerType: Int): ScanJob {
        return ScanJob(
            id = 1,
            name = "Job",
            description = null,
            status = "InProgress",
            type = "Scan",
            registerType = registerType
        )
    }

    private fun scanResultItem(
        extId: String?,
        count: Int = 1,
        hasIssues: Boolean = false
    ): ScanResultItem {
        return ScanResultItem(
            count = count,
            parcelCount = count,
            boxCount = 0,
            scanSource = ScannedItemSources.PARCEL_STICKER,
            itemNumbers = emptyList(),
            extData = null,
            extId = extId,
            hasIssues = hasIssues
        )
    }
}
