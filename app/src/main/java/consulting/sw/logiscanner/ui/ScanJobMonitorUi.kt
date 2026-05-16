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

enum class CheckStatusTone {
    NOT_CHECKED,
    APPROVED_WITH_EXCISE,
    APPROVED_WITH_NOTIFICATION,
    HAS_ISSUES_WITH_INHERITANCE,
    HAS_ISSUES,
    APPROVED_WITH_INHERITANCE,
    APPROVED,
    NO_ISSUES
}

data class CheckStatusTextSpec(
    val stringResIds: List<Int> = emptyList(),
    val fallbackHex: String? = null
)

data class MonitorParcelAttributeSpec(
    val labelResId: Int,
    val value: String? = null,
    val checkStatus: Int? = null
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

fun checkStatusTextSpec(checkStatus: Int): CheckStatusTextSpec {
    val fc = checkStatusFc(checkStatus)
    val sw = checkStatusSw(checkStatus)
    val special = when {
        fc == FC_NOT_CHECKED && sw == SW_NOT_CHECKED -> R.string.check_status_not_checked
        fc == FC_APPROVED_WITH_EXCISE && sw == SW_APPROVED_WITH_EXCISE -> R.string.check_status_approved_with_excise
        fc == FC_APPROVED_WITH_NOTIFICATION && sw == SW_APPROVED_WITH_NOTIFICATION -> R.string.check_status_approved_with_notification
        fc == FC_DUPLICATE && sw == SW_DUPLICATE -> R.string.check_status_duplicate
        fc == FC_NOT_FOUND && sw == SW_NOT_FOUND -> R.string.check_status_not_found
        fc == FC_MARKED_BY_PARTNER && sw == SW_MARKED_BY_PARTNER -> R.string.check_status_marked_by_partner
        else -> null
    }
    if (special != null) {
        return CheckStatusTextSpec(stringResIds = listOf(special))
    }

    val parts = listOfNotNull(swStatusStringResId(sw), fcStatusStringResId(fc))
    return if (parts.isNotEmpty()) {
        CheckStatusTextSpec(stringResIds = parts)
    } else {
        CheckStatusTextSpec(fallbackHex = checkStatus.toUInt().toString(16).uppercase().padStart(8, '0'))
    }
}

fun checkStatusText(context: Context, checkStatus: Int): String {
    val spec = checkStatusTextSpec(checkStatus)
    if (spec.fallbackHex != null) {
        return context.getString(R.string.check_status_unknown_hex, spec.fallbackHex)
    }
    return spec.stringResIds
        .map { context.getString(it) }
        .filter { it.isNotBlank() }
        .joinToString(context.getString(R.string.check_status_separator))
}

fun checkStatusTone(checkStatus: Int?): CheckStatusTone? {
    if (checkStatus == null) {
        return null
    }
    return when {
        checkStatus == checkStatusCompose(FC_NOT_CHECKED, SW_NOT_CHECKED) -> CheckStatusTone.NOT_CHECKED
        checkStatus == checkStatusCompose(FC_APPROVED_WITH_EXCISE, SW_APPROVED_WITH_EXCISE) -> CheckStatusTone.APPROVED_WITH_EXCISE
        checkStatus == checkStatusCompose(FC_APPROVED_WITH_NOTIFICATION, SW_APPROVED_WITH_NOTIFICATION) -> CheckStatusTone.APPROVED_WITH_NOTIFICATION
        checkStatusSw(checkStatus) == SW_ISSUE_STOP_WORD_INHERITED -> CheckStatusTone.HAS_ISSUES_WITH_INHERITANCE
        checkStatusHasIssues(checkStatus) -> CheckStatusTone.HAS_ISSUES
        checkStatusSw(checkStatus) == SW_APPROVED_INHERITED -> CheckStatusTone.APPROVED_WITH_INHERITANCE
        checkStatusSw(checkStatus) == SW_APPROVED -> CheckStatusTone.APPROVED
        else -> CheckStatusTone.NO_ISSUES
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
    parcel.checkStatus?.let {
        specs += MonitorParcelAttributeSpec(R.string.monitor_parcel_check_status, checkStatus = it)
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

private const val SW_INHERITANCE_FLAG = 0x0080

private const val SW_NOT_CHECKED = 0x0000
private const val SW_NO_ISSUES = 0x0010
private const val SW_APPROVED = 0x0020
private const val SW_APPROVED_INHERITED = SW_APPROVED or SW_INHERITANCE_FLAG
private const val SW_APPROVED_WITH_EXCISE = 0x0230
private const val SW_APPROVED_WITH_NOTIFICATION = 0x0231
private const val SW_ISSUE_STOP_WORD = 0x0100
private const val SW_ISSUE_STOP_WORD_INHERITED = SW_ISSUE_STOP_WORD or SW_INHERITANCE_FLAG
private const val SW_DUPLICATE = 0x017E
private const val SW_NOT_FOUND = 0x017D
private const val SW_MARKED_BY_PARTNER = 0x01FF

private const val FC_NOT_CHECKED = 0x0000
private const val FC_NO_ISSUES = 0x0010
private const val FC_APPROVED_WITH_EXCISE = 0x0230
private const val FC_APPROVED_WITH_NOTIFICATION = 0x0231
private const val FC_ISSUE_FEACN_CODE = 0x0100
private const val FC_ISSUE_NONEXISTING_FEACN = 0x0101
private const val FC_ISSUE_INVALID_FEACN_FORMAT = 0x0102
private const val FC_DUPLICATE = 0x017E
private const val FC_NOT_FOUND = 0x017D
private const val FC_MARKED_BY_PARTNER = 0x01FF

private fun checkStatusFc(checkStatus: Int): Int = (checkStatus ushr 16) and 0xFFFF

private fun checkStatusSw(checkStatus: Int): Int = checkStatus and 0xFFFF

private fun checkStatusCompose(fc: Int, sw: Int): Int = (fc shl 16) or sw

private fun checkStatusHasIssues(checkStatus: Int): Boolean {
    return (checkStatusFc(checkStatus) and 0x0100) != 0 || (checkStatusSw(checkStatus) and 0x0100) != 0
}

private fun swStatusStringResId(sw: Int): Int? {
    return when (sw) {
        SW_NO_ISSUES -> R.string.check_status_sw_no_issues
        SW_APPROVED -> R.string.check_status_approved
        SW_APPROVED_INHERITED -> R.string.check_status_approved
        SW_APPROVED_WITH_EXCISE -> R.string.check_status_approved_with_excise
        SW_APPROVED_WITH_NOTIFICATION -> R.string.check_status_approved_with_notification
        SW_ISSUE_STOP_WORD -> R.string.check_status_issue_stop_word
        SW_ISSUE_STOP_WORD_INHERITED -> R.string.check_status_issue_stop_word
        else -> null
    }
}

private fun fcStatusStringResId(fc: Int): Int? {
    return when (fc) {
        FC_NO_ISSUES -> R.string.check_status_fc_no_issues
        FC_APPROVED_WITH_EXCISE -> R.string.check_status_approved_with_excise
        FC_APPROVED_WITH_NOTIFICATION -> R.string.check_status_approved_with_notification
        FC_ISSUE_FEACN_CODE -> R.string.check_status_issue_feacn_code
        FC_ISSUE_NONEXISTING_FEACN -> R.string.check_status_issue_nonexisting_feacn
        FC_ISSUE_INVALID_FEACN_FORMAT -> R.string.check_status_issue_invalid_feacn_format
        else -> null
    }
}
