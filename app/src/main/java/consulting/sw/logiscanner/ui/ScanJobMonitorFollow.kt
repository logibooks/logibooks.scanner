// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorFollowTarget
import consulting.sw.logiscanner.repo.ScanJobMonitorScope

sealed class LocalScanFollowAction {
    object None : LocalScanFollowAction()
    object OpenRegister : LocalScanFollowAction()
    data class OpenDetail(
        val scope: ScanJobMonitorScope,
        val highlightedParcelId: Int?
    ) : LocalScanFollowAction()
}

fun localScanFollowAction(
    autoFollowEnabled: Boolean,
    target: ScanJobMonitorFollowTarget
): LocalScanFollowAction {
    if (!autoFollowEnabled) {
        return LocalScanFollowAction.None
    }

    return when (target.area) {
        ScanJobMonitorAreas.BOX -> {
            val boxId = target.boxId ?: return LocalScanFollowAction.None
            LocalScanFollowAction.OpenDetail(
                scope = ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = boxId),
                highlightedParcelId = target.parcelId
            )
        }
        ScanJobMonitorAreas.UNASSIGNED -> LocalScanFollowAction.OpenDetail(
            scope = ScanJobMonitorScope(
                ScanJobMonitorAreas.UNASSIGNED,
                bucketIndex = target.bucketIndex ?: 0
            ),
            highlightedParcelId = target.parcelId
        )
        ScanJobMonitorAreas.NOT_IN_REGISTER,
        ScanJobMonitorAreas.BOXES -> LocalScanFollowAction.OpenRegister
        else -> LocalScanFollowAction.None
    }
}
