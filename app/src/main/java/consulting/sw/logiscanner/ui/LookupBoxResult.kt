// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.net.LookupScanCandidate
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources

internal data class LookupBoxResult(
    val outstanding: List<LookupScanCandidate>,
    val confirmed: List<LookupScanCandidate>
)

internal fun ScanResultItem.isLookupBoxScan(): Boolean =
    scanSource == ScannedItemSources.BOX_STICKER && (lookupOutcome == "inspectBox" || lookupOutcome == "noOutstanding")

internal fun ScanResultItem.lookupBoxResult(): LookupBoxResult? =
    if (isLookupBoxScan()) LookupBoxResult(lookupCandidates.orEmpty(), confirmedLookupCandidates.orEmpty()) else null

internal fun List<LookupScanCandidate>.countsByParcelNumber(): Map<String, Int> =
    groupingBy { it.parcelNumber.ifBlank { "#${it.parcelId}" } }.eachCount()
