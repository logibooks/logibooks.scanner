// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.StickerTemplates
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
    fun stoppedScanWarningPreservesScanDataAndBusyState() {
        val state = MainState(
            isBusy = false,
            selectedScanJob = scanJob(registerType = RegisterTypes.WBR),
            isScanning = false,
            lastCode = "PREV",
            lastParcelCount = 1,
            lastBoxCount = 0,
            scanResultColor = ScanResultColor.OK
        )

        val warned = stoppedScanWarningState(state, "Stopped")

        assertFalse(warned.isBusy)
        assertFalse(warned.isScanning)
        assertEquals("PREV", warned.lastCode)
        assertEquals(1, warned.lastParcelCount)
        assertEquals(0, warned.lastBoxCount)
        assertEquals("Stopped", warned.error)
        assertEquals(ScanResultColor.IGNORED, warned.scanResultColor)
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
    fun scanHintTellsOperatorToStartWhenStopped() {
        assertEquals(
            R.string.scan_hint_stopped,
            scanHintResId(externalScannerEnabled = false, isScanning = false)
        )
        assertEquals(
            R.string.scan_hint_stopped,
            scanHintResId(externalScannerEnabled = true, isScanning = false)
        )
    }

    @Test
    fun scanTitleReflectsScanningState() {
        assertEquals(R.string.ready_to_scan, scanTitleResId(isScanning = true))
        assertEquals(R.string.scanning_stopped, scanTitleResId(isScanning = false))
    }

    @Test
    fun scanReceiverFollowsScanScreenVisibility() {
        val job = scanJob(registerType = RegisterTypes.WBR)

        assertTrue(scanReceiverEnabled(isLoggedIn = true, settingsOpen = false, selectedJob = job))
        assertFalse(scanReceiverEnabled(isLoggedIn = false, settingsOpen = false, selectedJob = job))
        assertFalse(scanReceiverEnabled(isLoggedIn = true, settingsOpen = true, selectedJob = job))
        assertFalse(scanReceiverEnabled(isLoggedIn = true, settingsOpen = false, selectedJob = null))
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
        assertFalse(bulkyItemsModeEnabled(scanJob(registerType = RegisterTypes.WBR_N)))
        assertFalse(bulkyItemsModeEnabled(scanJob(registerType = 1)))
        assertFalse(bulkyItemsModeEnabled(null))
    }

    @Test
    fun relabelingModeAvailabilityKeepsKgtWbrOnlyAndRequiresPrinterForFull() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val wbrNJob = scanJob(registerType = RegisterTypes.WBR_N)
        val tajikistanJob = scanJob(
            registerType = RegisterTypes.WBR_N,
            stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )
        val otherJob = scanJob(registerType = 1)

        assertTrue(relabelingModeAvailable(wbrJob, RelabelingSubmode.KGT, printerSelected = false))
        assertFalse(relabelingModeAvailable(wbrNJob, RelabelingSubmode.KGT, printerSelected = true))
        assertFalse(relabelingModeAvailable(otherJob, RelabelingSubmode.KGT, printerSelected = true))
        assertFalse(relabelingModeAvailable(null, RelabelingSubmode.KGT, printerSelected = true))
        assertFalse(relabelingModeAvailable(wbrJob, RelabelingSubmode.FULL, printerSelected = true))
        assertFalse(relabelingModeAvailable(wbrNJob, RelabelingSubmode.FULL, printerSelected = true))
        assertTrue(relabelingModeAvailable(tajikistanJob, RelabelingSubmode.FULL, printerSelected = true))
        assertFalse(relabelingModeAvailable(otherJob, RelabelingSubmode.FULL, printerSelected = true))
        assertFalse(relabelingModeAvailable(wbrJob, RelabelingSubmode.FULL, printerSelected = false))
        assertFalse(relabelingModeAvailable(tajikistanJob, RelabelingSubmode.FULL, printerSelected = false))
        assertFalse(relabelingModeAvailable(null, RelabelingSubmode.FULL, printerSelected = true))
    }

    @Test
    fun normalizeBulkyItemsModeRejectsNonWbrAndInvalidValues() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)

        assertEquals(BulkyItemsModes.SILENT, normalizeBulkyItemsMode(wbrJob, BulkyItemsModes.SILENT))
        assertEquals(BulkyItemsModes.NOTIFY, normalizeBulkyItemsMode(wbrJob, BulkyItemsModes.NOTIFY))
        assertEquals(BulkyItemsModes.OFF, normalizeBulkyItemsMode(wbrJob, 99))
        assertEquals(BulkyItemsModes.OFF, normalizeBulkyItemsMode(scanJob(registerType = RegisterTypes.WBR_N), BulkyItemsModes.NOTIFY))
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
        val tajikistanJob = scanJob(
            registerType = RegisterTypes.WBR_N,
            stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )

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
            BulkyItemsModes.OFF,
            normalizeRelabelingMode(
                otherJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.NOTIFY,
                voiceEnabled = true,
                printerSelected = true
            )
        )
        assertEquals(
            BulkyItemsModes.SILENT,
            normalizeRelabelingMode(
                tajikistanJob,
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
        val tajikistanJob = scanJob(
            registerType = RegisterTypes.WBR_N,
            stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )

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
                tajikistanJob,
                RelabelingSubmode.FULL,
                BulkyItemsModes.OFF,
                voiceEnabled = true,
                printerSelected = true
            )
        )
        assertEquals(
            BulkyItemsModes.OFF,
            nextRelabelingMode(
                tajikistanJob,
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
        val wbrNJob = scanJob(registerType = RegisterTypes.WBR_N)
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
                wbrNJob,
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
    fun kgtStickerCodeTrimsAndRejectsBlankValues() {
        assertEquals("15", kgtStickerCode(" 15 "))
        assertNull(kgtStickerCode(" "))
        assertNull(kgtStickerCode(null))
    }

    @Test
    fun canManualPrintKgtStickerRequiresCode() {
        assertTrue(canManualPrintKgtSticker("15"))
        assertTrue(canManualPrintKgtSticker("15", printerSelected = true))
        assertFalse(canManualPrintKgtSticker(" "))
        assertFalse(canManualPrintKgtSticker("15", printerSelected = false))
    }

    @Test
    fun shouldAutoPrintKgtStickerRequiresEnabledWbrModeAndGeneratedCode() {
        val wbrJob = scanJob(registerType = RegisterTypes.WBR)
        val result = scanResultItem(extId = "15")

        assertTrue(
            shouldAutoPrintKgtSticker(
                autoPrintEnabled = true,
                job = wbrJob,
                bulkyItemsMode = BulkyItemsModes.SILENT,
                result = result
            )
        )
        assertFalse(shouldAutoPrintKgtSticker(false, wbrJob, BulkyItemsModes.SILENT, result))
        assertFalse(shouldAutoPrintKgtSticker(true, wbrJob, BulkyItemsModes.OFF, result))
        assertFalse(shouldAutoPrintKgtSticker(true, scanJob(registerType = RegisterTypes.WBR_N), BulkyItemsModes.SILENT, result))
        assertFalse(shouldAutoPrintKgtSticker(true, scanJob(registerType = 1), BulkyItemsModes.SILENT, result))
        assertFalse(shouldAutoPrintKgtSticker(true, wbrJob, BulkyItemsModes.SILENT, result, printerSelected = false))
        assertFalse(shouldAutoPrintKgtSticker(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = null)))
        assertFalse(shouldAutoPrintKgtSticker(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = "15", count = 0)))
        assertFalse(shouldAutoPrintKgtSticker(true, wbrJob, BulkyItemsModes.SILENT, scanResultItem(extId = "15", hasIssues = true)))
    }

    @Test
    fun shouldAutoPrintTajikistanStickerRequiresExactTemplateAndEligibleParcelScan() {
        val job = scanJob(
            registerType = RegisterTypes.WBR_N,
            stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )
        val result = scanResultItem(
            extId = null,
            stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )

        assertTrue(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.KGT,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.OFF,
                printerSelected = true,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = false,
                job = job,
                result = result
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = scanResultItem(extId = null, stickerTemplate = "UNKNOWN")
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = scanResultItem(
                    extId = null,
                    count = 2,
                    stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
                )
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                result = scanResultItem(
                    extId = null,
                    hasIssues = true,
                    stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
                )
            )
        )
        assertFalse(
            shouldAutoPrintTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = scanJob(registerType = RegisterTypes.WBR_N),
                result = result
            )
        )
    }

    @Test
    fun repeatTajikistanStickerRequiresActiveFullModePrinterAndStoredSticker() {
        val job = scanJob(
            registerType = RegisterTypes.WBR_N,
            stickerTemplate = StickerTemplates.TAJIKISTAN_EXPORT
        )
        assertTrue(
            canRepeatTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                printerSelected = true,
                job = job,
                hasSticker = true
            )
        )
        assertFalse(canRepeatTajikistanExportSticker(RelabelingSubmode.KGT, BulkyItemsModes.SILENT, true, job, true))
        assertFalse(canRepeatTajikistanExportSticker(RelabelingSubmode.FULL, BulkyItemsModes.OFF, true, job, true))
        assertFalse(canRepeatTajikistanExportSticker(RelabelingSubmode.FULL, BulkyItemsModes.SILENT, false, job, true))
        assertFalse(canRepeatTajikistanExportSticker(RelabelingSubmode.FULL, BulkyItemsModes.SILENT, true, job, false))
        assertFalse(
            canRepeatTajikistanExportSticker(
                RelabelingSubmode.FULL,
                BulkyItemsModes.SILENT,
                true,
                scanJob(registerType = RegisterTypes.WBR_N),
                true
            )
        )
    }

    private fun scanJob(registerType: Int, stickerTemplate: String? = null): ScanJob {
        return ScanJob(
            id = 1,
            name = "Job",
            description = null,
            status = "InProgress",
            type = "Scan",
            registerType = registerType,
            stickerTemplate = stickerTemplate
        )
    }

    private fun scanResultItem(
        extId: String?,
        count: Int = 1,
        hasIssues: Boolean = false,
        scanSource: Int = ScannedItemSources.PARCEL_STICKER,
        followTarget: ScanJobMonitorFollowTarget = ScanJobMonitorFollowTarget(),
        stickerTemplate: String? = null
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
            followTarget = followTarget,
            stickerTemplate = stickerTemplate
        )
    }
}
