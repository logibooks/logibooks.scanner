// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.annotation.StringRes
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.LookupScanOutcome
import consulting.sw.logiscanner.net.LookupTargetModes
import consulting.sw.logiscanner.net.ScanJobOperations
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScopedLookupProgress

@StringRes
internal fun lookupPresetLabel(targetMode: Int?, operation: Int?): Int? = when (targetMode) {
    LookupTargetModes.RED_ZONE_AT_START -> R.string.lookup_preset_red
    LookupTargetModes.MISSING_FROM_JOB -> R.string.lookup_preset_missing
    else -> R.string.lookup_preset_status.takeIf { operation == ScanJobOperations.LOOKUP }
}

data class LookupScanFeedback(@param:StringRes val label: Int, val candidateParcelNumbers: List<String> = emptyList())

// Target membership is frozen at start and confirmations can only increase.
internal fun mergeLookupProgress(
    current: ScopedLookupProgress?,
    incoming: ScopedLookupProgress?
): ScopedLookupProgress? {
    if (incoming == null) return current
    if (current == null || incoming.expectedCount > current.expectedCount) return incoming
    if (incoming.expectedCount < current.expectedCount || incoming.confirmedCount < current.confirmedCount) return current
    if (incoming.confirmedCount == current.confirmedCount && incoming.missingParcels == null) return current
    return incoming
}

internal fun lookupScanFeedback(result: ScanResultItem): LookupScanFeedback? = when (result.lookupOutcome) {
    LookupScanOutcome.CONFIRMED -> LookupScanFeedback(R.string.lookup_confirmed)
    LookupScanOutcome.INSPECT_BOX -> LookupScanFeedback(R.string.lookup_inspect_box, result.candidateParcelNumbers.orEmpty())
    LookupScanOutcome.ALREADY_CONFIRMED -> LookupScanFeedback(R.string.lookup_already_confirmed)
    else -> null
}
