// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.RegisterTypes
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanResultItem

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

fun kgtLabelCode(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

fun canManualPrintKgtLabel(value: String?): Boolean = kgtLabelCode(value) != null

fun shouldAutoPrintKgtLabel(
    autoPrintEnabled: Boolean,
    job: ScanJob?,
    bulkyItemsMode: Int,
    result: ScanResultItem
): Boolean {
    return autoPrintEnabled
        && normalizeBulkyItemsMode(job, bulkyItemsMode) != BulkyItemsModes.OFF
        && result.count > 0
        && !result.hasIssues
        && kgtLabelCode(result.extId) != null
}
