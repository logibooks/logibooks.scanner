// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.content.Context
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ParcelCheckStatusProjection
import consulting.sw.logiscanner.net.ParcelCheckStatusProjectionKinds
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val monitorDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")
private val monitorLatestScanTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val monitorLatestScanDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

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

fun formatMonitorParcelProgress(total: Int, scanned: Int, notScanned: Int, restricted: Int): String {
    return "$total / $scanned / $notScanned / $restricted"
}

fun formatMonitorTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(monitorDateTimeFormatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value).format(monitorDateTimeFormatter)
        } catch (_: DateTimeParseException) {
            value
        }
    }
}

fun formatMonitorQuantity(value: Double): String {
    val longValue = value.toLong()
    return if (value == longValue.toDouble()) {
        longValue.toString()
    } else {
        value.toString()
    }
}

fun formatMonitorLatestScanTime(value: String?): String {
    return formatMonitorDateTimePart(value, monitorLatestScanTimeFormatter)
}

fun formatMonitorLatestScanDate(value: String?): String {
    return formatMonitorDateTimePart(value, monitorLatestScanDateFormatter)
}

fun monitorLatestScanCode(snapshot: ScanJobMonitorSnapshot?, fallbackCode: String?): String {
    val monitorCode = snapshot?.latestScan?.code?.takeIf { it.isNotBlank() }
    return monitorCode ?: fallbackCode.orEmpty()
}

fun directScanResultCode(snapshot: ScanJobMonitorSnapshot?, lastCode: String?): String? {
    val monitorCode = snapshot?.latestScan?.code?.takeIf { it.isNotBlank() } ?: return null
    return lastCode?.takeIf { it.isNotBlank() && it != monitorCode }
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

data class MonitorParcelAttributeSpec(
    val labelResId: Int,
    val value: String? = null,
    val checkStatusProjection: ParcelCheckStatusProjection? = null
)

enum class MonitorLatestScanNumberKind {
    PARCEL,
    BOX
}

data class MonitorLatestScanDisplay(
    val code: String?,
    val scanTime: String?,
    val parcelCount: Int,
    val boxCount: Int,
    val scanSource: Int?,
    val itemNumbers: List<String>,
    val hint: String?
) {
    val numberKind: MonitorLatestScanNumberKind?
        get() {
            if (itemNumbers.isEmpty()) {
                return null
            }
            return when (scanSource) {
                ScannedItemSources.PARCEL_STICKER -> MonitorLatestScanNumberKind.PARCEL
                ScannedItemSources.BOX_STICKER -> MonitorLatestScanNumberKind.BOX
                else -> null
            }
        }
}

fun monitorLatestScanDisplay(
    snapshot: ScanJobMonitorSnapshot?,
    lastCode: String?,
    lastParcelCount: Int?,
    lastBoxCount: Int?,
    lastScanSource: Int?,
    lastItemNumbers: List<String>,
    lastExtData: String?,
    lastScanTime: String?
): MonitorLatestScanDisplay? {
    val monitorScan = snapshot?.latestScan?.takeIf { it.code.isNotBlank() }
    val localCode = lastCode?.takeIf { it.isNotBlank() }
    val localMatchesMonitor = monitorScan != null && localCode == monitorScan.code
    val useLocalScanResult = monitorScan == null
    val code = monitorScan?.code ?: localCode
    val scanTime = monitorScan?.scanTime?.takeIf { it.isNotBlank() }
        ?: lastScanTime?.takeIf { it.isNotBlank() }?.takeIf { useLocalScanResult }
    val hint = lastExtData?.takeIf { it.isNotBlank() && (useLocalScanResult || localMatchesMonitor) }

    if (
        code == null
        && lastParcelCount == null
        && lastBoxCount == null
        && hint == null
        && scanTime == null
    ) {
        return null
    }

    return if (useLocalScanResult) {
        MonitorLatestScanDisplay(
            code = code,
            scanTime = scanTime,
            parcelCount = lastParcelCount ?: 0,
            boxCount = lastBoxCount ?: 0,
            scanSource = lastScanSource,
            itemNumbers = lastItemNumbers,
            hint = hint
        )
    } else {
        MonitorLatestScanDisplay(
            code = code,
            scanTime = scanTime,
            parcelCount = monitorScan.parcelCount,
            boxCount = monitorScan.boxCount,
            scanSource = monitorScan.scanSource,
            itemNumbers = monitorScan.itemNumbers,
            hint = hint
        )
    }
}

fun monitorParcelAttributeSpecs(parcel: ScanJobMonitorParcel): List<MonitorParcelAttributeSpec> {
    val specs = mutableListOf<MonitorParcelAttributeSpec>()
    parcel.scannedSticker?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_scanned_sticker, it)
    }
    if (parcel.scannedUserName.isNotBlank()) {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_scanned_user, parcel.scannedUserName)
    }
    parcel.scannedTime?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_scanned_time, formatMonitorTime(it))
    }
    parcel.shk?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_shk, it)
    }
    parcel.sticker?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_sticker, it)
    }
    parcel.wbSticker?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_wb_sticker, it)
    }
    parcel.sellerSticker?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_seller_sticker, it)
    }
    parcel.stickerCode?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_sticker_code, it)
    }
    parcel.postingNumber?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_posting_number, it)
    }
    parcel.barcode?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_barcode, it)
    }
    parcel.weightKg?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_weight_kg, it.toString())
    }
    parcel.quantity?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_quantity, formatMonitorQuantity(it))
    }
    if (parcel.zoneName.isNotBlank()) {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_zone_name, parcel.zoneName)
    }
    if (parcel.statusTitle.isNotBlank()) {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_status_title, parcel.statusTitle)
    }
    parcel.checkStatusProjection?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_check_status, checkStatusProjection = it)
    }
    return specs
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

fun isRestrictedMonitorParcel(parcel: ScanJobMonitorParcel): Boolean {
    return parcel.checkStatusProjection?.kind == ParcelCheckStatusProjectionKinds.RESTRICTION
}

fun parcelSecondaryText(parcel: ScanJobMonitorParcel): String {
    return firstNotBlank(parcel.productName, parcel.statusTitle, parcel.zoneName) ?: ""
}

private fun firstNotBlank(vararg values: String?): String? {
    return values.firstOrNull { !it.isNullOrBlank() }
}

private fun formatMonitorDateTimePart(value: String?, formatter: DateTimeFormatter): String {
    if (value.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(formatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value).format(formatter)
        } catch (_: DateTimeParseException) {
            ""
        }
    }
}

