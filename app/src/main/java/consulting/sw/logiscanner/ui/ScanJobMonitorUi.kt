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
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val monitorDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")
private val localScanResultTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val localScanResultDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

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

fun formatLocalScanResultTime(value: String?): String {
    return formatMonitorDateTimePart(value, localScanResultTimeFormatter)
}

fun formatLocalScanResultDate(value: String?): String {
    return formatMonitorDateTimePart(value, localScanResultDateFormatter)
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

enum class LocalScanResultNumberKind {
    PARCEL,
    BOX
}

data class LocalScanResultDisplay(
    val code: String?,
    val scanTime: String?,
    val parcelCount: Int,
    val boxCount: Int,
    val scanSource: Int?,
    val itemNumbers: List<String>,
    val hint: String?,
    /** WBR bulky item number shown for the latest BI-mode parcel scan. */
    val extId: String?
) {
    val numberKind: LocalScanResultNumberKind?
        get() {
            if (itemNumbers.isEmpty()) {
                return null
            }
            return when (scanSource) {
                ScannedItemSources.PARCEL_STICKER -> LocalScanResultNumberKind.PARCEL
                ScannedItemSources.BOX_STICKER -> LocalScanResultNumberKind.BOX
                else -> null
            }
        }
}

fun localScanResultDisplay(
    lastCode: String?,
    lastParcelCount: Int?,
    lastBoxCount: Int?,
    lastScanSource: Int?,
    lastItemNumbers: List<String>,
    lastExtData: String?,
    lastExtId: String?,
    lastScanTime: String?
): LocalScanResultDisplay? {
    val localCode = lastCode?.takeIf { it.isNotBlank() }
    val scanTime = lastScanTime?.takeIf { it.isNotBlank() }
    val hint = lastExtData?.takeIf { it.isNotBlank() }
    val extId = lastExtId?.takeIf { it.isNotBlank() }

    if (
        localCode == null
        && lastParcelCount == null
        && lastBoxCount == null
        && hint == null
        && extId == null
        && scanTime == null
    ) {
        return null
    }

    return LocalScanResultDisplay(
        code = localCode,
        scanTime = scanTime,
        parcelCount = lastParcelCount ?: 0,
        boxCount = lastBoxCount ?: 0,
        scanSource = lastScanSource,
        itemNumbers = lastItemNumbers,
        hint = hint,
        extId = extId
    )
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
    parcel.extId?.takeIf { it.isNotBlank() }?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_ext_id, it)
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
        val barcodeLabel = if (parcel.postingNumber.isNullOrBlank()) {
            R.string.monitor_parcel_wbr_barcode
        } else {
            R.string.monitor_parcel_barcode
        }
        specs += MonitorParcelAttributeSpec(barcodeLabel, it)
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
    return isIssueCheckStatusProjectionKind(parcel.checkStatusProjection?.kind)
}

fun isIssueCheckStatusProjectionKind(kind: Int?): Boolean {
    return when (kind) {
        ParcelCheckStatusProjectionKinds.RESTRICTION,
        ParcelCheckStatusProjectionKinds.DEFECT -> true
        else -> false
    }
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

