// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.store.RelabelingSubmode

fun scanHintResId(externalScannerEnabled: Boolean): Int {
    return if (externalScannerEnabled) {
        R.string.scan_hint_external_enabled
    } else {
        R.string.scan_hint_hardware_only
    }
}

fun hidScannerInputEnabled(externalScannerEnabled: Boolean): Boolean = externalScannerEnabled

fun hidFocusRecoverySuspended(
    externalScannerEnabled: Boolean,
    textFieldFocused: Boolean
): Boolean = externalScannerEnabled && textFieldFocused

/** Returns true when the selected scan job may use BI mode. */
fun bulkyItemsModeEnabled(job: ScanJob?): Boolean {
    return job?.registerType == RegisterTypes.WBR
}

/** Forces BI mode to Off for non-WBR jobs and invalid values. */
fun normalizeBulkyItemsMode(job: ScanJob?, mode: Int): Int {
    return if (bulkyItemsModeEnabled(job) && mode in BulkyItemsModes.OFF..BulkyItemsModes.NOTIFY) {
        mode
    } else {
        BulkyItemsModes.OFF
    }
}

/** Returns the backend mode for enabled BI mode using the current voice setting. */
fun enabledBulkyItemsMode(voiceEnabled: Boolean): Int {
    return if (voiceEnabled) {
        BulkyItemsModes.NOTIFY
    } else {
        BulkyItemsModes.SILENT
    }
}

/** Toggles BI mode as a two-state Off/On option for WBR scan jobs. */
fun nextBulkyItemsMode(job: ScanJob?, currentMode: Int, voiceEnabled: Boolean): Int {
    if (!bulkyItemsModeEnabled(job)) {
        return BulkyItemsModes.OFF
    }

    return when (normalizeBulkyItemsMode(job, currentMode)) {
        BulkyItemsModes.OFF -> enabledBulkyItemsMode(voiceEnabled)
        else -> BulkyItemsModes.OFF
    }
}

/** Returns true when BI mode should announce the returned ExtId. */
fun bulkyItemsModeNotifies(mode: Int, voiceEnabled: Boolean): Boolean {
    return voiceEnabled && mode != BulkyItemsModes.OFF
}

fun applyBulkyItemsVoiceSetting(mode: Int, voiceEnabled: Boolean): Int {
    return when (mode) {
        BulkyItemsModes.SILENT,
        BulkyItemsModes.NOTIFY -> enabledBulkyItemsMode(voiceEnabled)
        else -> BulkyItemsModes.OFF
    }
}

fun hasSelectedPrinter(address: String?): Boolean = !address.isNullOrBlank()

fun relabelingModeAvailable(
    job: ScanJob?,
    submode: RelabelingSubmode,
    printerSelected: Boolean
): Boolean {
    return when (submode) {
        RelabelingSubmode.KGT -> bulkyItemsModeEnabled(job)
        RelabelingSubmode.FULL -> job != null && printerSelected
    }
}

fun relabelingModeNotifies(
    submode: RelabelingSubmode,
    mode: Int,
    voiceEnabled: Boolean
): Boolean {
    return submode == RelabelingSubmode.KGT && bulkyItemsModeNotifies(mode, voiceEnabled)
}

fun applyRelabelingVoiceSetting(
    submode: RelabelingSubmode,
    mode: Int,
    voiceEnabled: Boolean
): Int {
    return when (submode) {
        RelabelingSubmode.KGT -> applyBulkyItemsVoiceSetting(mode, voiceEnabled)
        RelabelingSubmode.FULL -> if (mode == BulkyItemsModes.OFF) {
            BulkyItemsModes.OFF
        } else {
            BulkyItemsModes.SILENT
        }
    }
}

fun normalizeRelabelingMode(
    job: ScanJob?,
    submode: RelabelingSubmode,
    mode: Int,
    voiceEnabled: Boolean,
    printerSelected: Boolean
): Int {
    if (!relabelingModeAvailable(job, submode, printerSelected)) {
        return BulkyItemsModes.OFF
    }

    return when (submode) {
        RelabelingSubmode.KGT -> normalizeBulkyItemsMode(
            job,
            applyBulkyItemsVoiceSetting(mode, voiceEnabled)
        )
        RelabelingSubmode.FULL -> if (mode == BulkyItemsModes.OFF) {
            BulkyItemsModes.OFF
        } else {
            BulkyItemsModes.SILENT
        }
    }
}

fun nextRelabelingMode(
    job: ScanJob?,
    submode: RelabelingSubmode,
    currentMode: Int,
    voiceEnabled: Boolean,
    printerSelected: Boolean
): Int {
    if (!relabelingModeAvailable(job, submode, printerSelected)) {
        return BulkyItemsModes.OFF
    }

    val normalized = normalizeRelabelingMode(job, submode, currentMode, voiceEnabled, printerSelected)
    if (normalized != BulkyItemsModes.OFF) {
        return BulkyItemsModes.OFF
    }

    return when (submode) {
        RelabelingSubmode.KGT -> enabledBulkyItemsMode(voiceEnabled)
        RelabelingSubmode.FULL -> BulkyItemsModes.SILENT
    }
}

fun backendBulkyItemsMode(
    job: ScanJob?,
    submode: RelabelingSubmode,
    relabelingMode: Int,
    voiceEnabled: Boolean
): Int {
    return if (submode == RelabelingSubmode.KGT) {
        normalizeBulkyItemsMode(job, applyBulkyItemsVoiceSetting(relabelingMode, voiceEnabled))
    } else {
        BulkyItemsModes.OFF
    }
}

fun kgtLabelCode(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

fun canManualPrintKgtLabel(value: String?, printerSelected: Boolean = true): Boolean {
    return printerSelected && kgtLabelCode(value) != null
}

fun shouldAutoPrintKgtLabel(
    autoPrintEnabled: Boolean,
    job: ScanJob?,
    bulkyItemsMode: Int,
    result: ScanResultItem,
    printerSelected: Boolean = true
): Boolean {
    return autoPrintEnabled
        && printerSelected
        && normalizeBulkyItemsMode(job, bulkyItemsMode) != BulkyItemsModes.OFF
        && result.count > 0
        && !result.hasIssues
        && kgtLabelCode(result.extId) != null
}

fun shouldAutoPrintFullRelabelingLabel(
    submode: RelabelingSubmode,
    relabelingMode: Int,
    printerSelected: Boolean,
    job: ScanJob?,
    result: ScanResultItem
): Boolean {
    return submode == RelabelingSubmode.FULL
        && relabelingMode != BulkyItemsModes.OFF
        && printerSelected
        && job != null
        && job.registerId > 0
        && result.count == 1
        && result.parcelCount == 1
        && result.scanSource == ScannedItemSources.PARCEL_STICKER
        && !result.hasIssues
        && (result.followTarget.parcelId ?: 0) > 0
}
