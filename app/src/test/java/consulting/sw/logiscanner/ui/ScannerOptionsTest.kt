// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorFollowTarget
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.store.RelabelingSubmode
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
            kgtVoiceEnabled = true,
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
    fun mainStateDefaultsKgtVoiceDisabled() {
        assertFalse(MainState().kgtVoiceEnabled)
    }

    @Test
    fun mainStateDefaultsRelabelingSubmodeKgt() {
        assertEquals(RelabelingSubmode.KGT, MainState().relabelingSubmode)
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
    fun relabelingModeAvailabilityKeepsKgtWbrOnlyAndRequiresPrinterForFull() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val otherJob = scanJob(registerType = 1)

        assertTrue(relabelingModeAvailable(wbrJob, RelabelingSubmode.KGT, printerSelected = false))
        assertFalse(relabelingModeAvailable(otherJob, RelabelingSubmode.KGT, printerSelected = true))
        assertFalse(relabelingModeAvailable(null, RelabelingSubmode.KGT, printerSelected = true))
        assertTrue(relabelingModeAvailable(wbrJob, RelabelingSubmode.FULL, printerSelected = true))
        assertTrue(relabelingModeAvailable(otherJob, RelabelingSubmode.FULL, printerSelected = true))
        assertFalse(relabelingModeAvailable(wbrJob, RelabelingSubmode.FULL, printerSelected = false))
        assertFalse(relabelingModeAvailable(null, RelabelingSubmode.FULL, printerSelected = true))
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
    fun nextBulkyItemsModeTogglesOnlyForWbr() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)

        assertEquals(
            BulkyItemsModes.SILENT,
            nextBulkyItemsMode(wbrJob, BulkyItemsModes.OFF, voiceEnabled = false)
        )
        assertEquals(
            BulkyItemsModes.NOTIFY,
            nextBulkyItemsMode(wbrJob, BulkyItemsModes.OFF, voiceEnabled = true)
        )
        assertEquals(
            BulkyItemsModes.OFF,
            nextBulkyItemsMode(wbrJob, BulkyItemsModes.SILENT, voiceEnabled = false)
        )
        assertEquals(
            BulkyItemsModes.OFF,
            nextBulkyItemsMode(wbrJob, BulkyItemsModes.NOTIFY, voiceEnabled = true)
        )
        assertEquals(
            BulkyItemsModes.OFF,
            nextBulkyItemsMode(scanJob(registerType = 1), BulkyItemsModes.SILENT, voiceEnabled = false)
        )
    }

    @Test
    fun bulkyItemsVoiceSettingControlsEnabledBackendMode() {
        assertEquals(BulkyItemsModes.SILENT, enabledBulkyItemsMode(voiceEnabled = false))
        assertEquals(BulkyItemsModes.NOTIFY, enabledBulkyItemsMode(voiceEnabled = true))
        assertEquals(
            BulkyItemsModes.OFF,
            applyBulkyItemsVoiceSetting(BulkyItemsModes.OFF, voiceEnabled = true)
        )
        assertEquals(
            BulkyItemsModes.OFF,
            applyBulkyItemsVoiceSetting(99, voiceEnabled = true)
        )
        assertEquals(
            BulkyItemsModes.SILENT,
            applyBulkyItemsVoiceSetting(BulkyItemsModes.NOTIFY, voiceEnabled = false)
        )
        assertEquals(
            BulkyItemsModes.NOTIFY,
            applyBulkyItemsVoiceSetting(BulkyItemsModes.SILENT, voiceEnabled = true)
        )
        assertFalse(bulkyItemsModeNotifies(BulkyItemsModes.SILENT, voiceEnabled = false))
        assertTrue(bulkyItemsModeNotifies(BulkyItemsModes.SILENT, voiceEnabled = true))
        assertFalse(bulkyItemsModeNotifies(BulkyItemsModes.OFF, voiceEnabled = true))
    }

    @Test
    fun normalizeRelabelingModeRejectsUnavailableModesAndDisablesFullVoice() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val otherJob = scanJob(registerType = 1)

        assertEquals(
            BulkyItemsModes.NOTIFY,
            normalizeRelabelingMode(
                wbrJob,
                RelabelingSubmode.KGT,
                BulkyItemsModes.SILENT,
                voiceEnabled = true,
                printerSelected = false
            )
        )
        assertEquals(
            BulkyItemsModes.OFF,
            normalizeRelabelingMode(
                otherJob,
                RelabelingSubmode.KGT,
                BulkyItemsModes.SILENT,
                voiceEnabled = false,
                printerSelected = true
            )
        )
        assertEquals(
            BulkyItemsModes.SILENT,
            normalizeRelabelingMode(
                otherJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.NOTIFY,
                voiceEnabled = true,
                printerSelected = true
            )
        )
        assertEquals(
            BulkyItemsModes.OFF,
            normalizeRelabelingMode(
                otherJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                voiceEnabled = true,
                printerSelected = false
            )
        )
    }

    @Test
    fun nextRelabelingModeUsesKgtVoiceAndFullSilentMode() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val otherJob = scanJob(registerType = 1)

        assertEquals(
            BulkyItemsModes.NOTIFY,
            nextRelabelingMode(
                wbrJob,
                RelabelingSubmode.KGT,
                BulkyItemsModes.OFF,
                voiceEnabled = true,
                printerSelected = false
            )
        )
        assertEquals(
            BulkyItemsModes.SILENT,
            nextRelabelingMode(
                otherJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.OFF,
                voiceEnabled = true,
                printerSelected = true
            )
        )
        assertEquals(
            BulkyItemsModes.OFF,
            nextRelabelingMode(
                otherJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                voiceEnabled = true,
                printerSelected = true
            )
        )
        assertEquals(
            BulkyItemsModes.OFF,
            nextRelabelingMode(
                otherJob,
                RelabelingSubmode.KGT,
                BulkyItemsModes.OFF,
                voiceEnabled = true,
                printerSelected = true
            )
        )
    }

    @Test
    fun backendBulkyItemsModeKeepsKgtAndTurnsFullModeOff() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val otherJob = scanJob(registerType = 1)

        assertEquals(
            BulkyItemsModes.NOTIFY,
            backendBulkyItemsMode(
                wbrJob,
                RelabelingSubmode.KGT,
                BulkyItemsModes.SILENT,
                voiceEnabled = true
            )
        )
        assertEquals(
            BulkyItemsModes.OFF,
            backendBulkyItemsMode(
                otherJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                voiceEnabled = true
            )
        )
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
        assertTrue(canManualPrintKgtLabel("15", printerSelected = true))
        assertFalse(canManualPrintKgtLabel(" "))
        assertFalse(canManualPrintKgtLabel("15", printerSelected = false))
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
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, result, printerSelected = false))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = null)))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = "15", count = 0)))
        assertFalse(shouldAutoPrintKgtLabel(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = "15", hasIssues = true)))
    }

    @Test
    fun shouldAutoPrintFullRelabelingLabelRequiresFullModePrinterAndResolvedParcel() {
        val job = scanJob(registerType = 1, registerId = 45)
        val result = scanResultItem(
            extId = null,
            followTarget = ScanJobMonitorFollowTarget(parcelId = 123)
        )

        assertTrue(
            shouldAutoPrintFullRelabelingLabel(
                submode = RelabelingSubmode.FULL,
                relabelingMode = BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.KGT,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.FULL,
                BulkyItemsModes.OFF,
                printerSelected = true,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = false,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = scanJob(registerType = 1, registerId = 0),
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = scanResultItem(
                    extId = null,
                    followTarget = ScanJobMonitorFollowTarget(parcelId = null)
                )
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = scanResultItem(
                    extId = null,
                    count = 2,
                    followTarget = ScanJobMonitorFollowTarget(parcelId = 123)
                )
            )
        )
        assertFalse(
            shouldAutoPrintFullRelabelingLabel(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = scanResultItem(
                    extId = null,
                    scanSource = ScannedItemSources.BOX_STICKER,
                    followTarget = ScanJobMonitorFollowTarget(parcelId = 123)
                )
            )
        )
    }

    private fun scanJob(registerType: Int, registerId: Int = 10): ScanJob {
        return ScanJob(
            id = 1,
            name = "Job",
            description = null,
            status = "InProgress",
            type = "Scan",
            registerId = registerId,
            registerType = registerType
        )
    }

    private fun scanResultItem(
        extId: String?,
        count: Int = 1,
        hasIssues: Boolean = false,
        scanSource: Int = ScannedItemSources.PARCEL_STICKER,
        followTarget: ScanJobMonitorFollowTarget = ScanJobMonitorFollowTarget()
    ): ScanResultItem {
        return ScanResultItem(
            count = count,
            parcelCount = count,
            boxCount = 0,
            scanSource = scanSource,
            itemNumbers = emptyList(),
            extData = null,
            extId = extId,
            hasIssues = hasIssues,
            followTarget = followTarget
        )
    }
}
