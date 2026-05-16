// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.content.Context
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val monitorDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

fun isUnassignedMonitorBox(box: ScanJobMonitorBox?): Boolean {
    return box?.area == ScanJobMonitorAreas.UNASSIGNED || (box?.boxId == null && box?.bucketIndex != null)
}

fun monitorBoxDisplayName(context: Context, box: ScanJobMonitorBox?): String {
    if (box == null) return ""
    if (isUnassignedMonitorBox(box)) {
        return box.boxCode.ifBlank {
            val idx = box.bucketIndex
            if (idx != null) {
                context.getString(R.string.monitor_unassigned_group_numbered, idx + 1)
            } else {
                context.getString(R.string.monitor_unassigned_group)
            }
        }
    }
    return box.boxCode.ifBlank {
        context.getString(R.string.monitor_box_display_name, box.boxId?.toString().orEmpty()).trim()
    }
}

fun monitorScopeForBox(box: ScanJobMonitorBox): ScanJobMonitorScope? {
    return if (isUnassignedMonitorBox(box)) {
        ScanJobMonitorScope(
            area = ScanJobMonitorAreas.UNASSIGNED,
            bucketIndex = box.bucketIndex ?: 0
        )
    } else {
        val boxId = box.boxId ?: return null
        ScanJobMonitorScope(
            area = ScanJobMonitorAreas.BOX,
            boxId = boxId
        )
    }
}

fun latestScanScope(snapshot: ScanJobMonitorSnapshot): ScanJobMonitorScope? {
    val latestScan = snapshot.latestScan ?: return null
    return when (latestScan.area) {
        ScanJobMonitorAreas.BOX -> {
            val boxId = latestScan.boxId ?: return null
            ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = boxId)
        }
        ScanJobMonitorAreas.UNASSIGNED -> {
            ScanJobMonitorScope(
                ScanJobMonitorAreas.UNASSIGNED,
                bucketIndex = latestScan.bucketIndex ?: 0
            )
        }
        ScanJobMonitorAreas.NOT_IN_REGISTER,
        ScanJobMonitorAreas.BOXES -> ScanJobMonitorScope(ScanJobMonitorAreas.BOXES)
        else -> null
    }
}

fun sameMonitorScope(left: ScanJobMonitorScope, right: ScanJobMonitorScope): Boolean {
    return left.area == right.area
        && left.boxId == right.boxId
        && (left.bucketIndex ?: 0) == (right.bucketIndex ?: 0)
}

fun formatMonitorProgress(total: Int, scanned: Int, notScanned: Int): String {
    return "$total / $scanned / $notScanned"
}

fun formatMonitorTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(value).format(monitorDateTimeFormatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value).format(monitorDateTimeFormatter)
        } catch (_: DateTimeParseException) {
            value
        }
    }
}

fun scanJobStatusText(context: Context, status: Int?): String {
    return when (status) {
        10 -> context.getString(R.string.scan_job_status_created)
        15 -> context.getString(R.string.scan_job_status_in_progress)
        18 -> context.getString(R.string.scan_job_status_paused)
        20 -> context.getString(R.string.scan_job_status_completed)
        null -> context.getString(R.string.scan_job_status_unknown)
        else -> status.toString()
    }
}

fun parcelPrimaryText(parcel: ScanJobMonitorParcel): String {
    return firstNotBlank(
        parcel.parcelNumber,
        parcel.postingNumber,
        parcel.shk,
        parcel.sticker,
        parcel.wbSticker,
        parcel.sellerSticker,
        parcel.stickerCode,
        parcel.barcode
    ) ?: "-"
}

fun parcelSecondaryText(parcel: ScanJobMonitorParcel): String {
    return firstNotBlank(parcel.productName, parcel.statusTitle, parcel.zoneName) ?: ""
}

private fun firstNotBlank(vararg values: String?): String? {
    return values.firstOrNull { !it.isNullOrBlank() }
}
