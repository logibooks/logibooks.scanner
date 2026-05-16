// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorLatestScan
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanJobMonitorUiTest {

    @Test
    fun isUnassignedMonitorBox_detectsAreaAndBucket() {
        assertTrue(isUnassignedMonitorBox(ScanJobMonitorBox(area = ScanJobMonitorAreas.UNASSIGNED)))
        assertTrue(isUnassignedMonitorBox(ScanJobMonitorBox(boxId = null, bucketIndex = 2)))
        assertFalse(isUnassignedMonitorBox(ScanJobMonitorBox(area = ScanJobMonitorAreas.BOX, boxId = 7)))
    }

    @Test
    fun latestScanScope_returnsBoxScope() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(
                scanCodeId = 10,
                area = ScanJobMonitorAreas.BOX,
                boxId = 42
            )
        )

        assertEquals(ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = 42), latestScanScope(snapshot))
    }

    @Test
    fun latestScanScope_returnsUnassignedScopeWithDefaultBucket() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(
                scanCodeId = 10,
                area = ScanJobMonitorAreas.UNASSIGNED,
                bucketIndex = null
            )
        )

        assertEquals(
            ScanJobMonitorScope(ScanJobMonitorAreas.UNASSIGNED, bucketIndex = 0),
            latestScanScope(snapshot)
        )
    }

    @Test
    fun latestScanScope_returnsRegisterScopeForNotInRegister() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(
                scanCodeId = 10,
                area = ScanJobMonitorAreas.NOT_IN_REGISTER
            )
        )

        assertEquals(ScanJobMonitorScope(ScanJobMonitorAreas.BOXES), latestScanScope(snapshot))
    }

    @Test
    fun latestScanScope_returnsNullWhenBoxIdMissing() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(
                scanCodeId = 10,
                area = ScanJobMonitorAreas.BOX,
                boxId = null
            )
        )

        assertNull(latestScanScope(snapshot))
    }

    @Test
    fun parcelPrimaryText_usesBestIdentifier() {
        val parcel = ScanJobMonitorParcel(
            parcelNumber = "",
            postingNumber = "POST-1",
            barcode = "BAR-1"
        )

        assertEquals("POST-1", parcelPrimaryText(parcel))
    }

    @Test
    fun formatMonitorTime_formatsIsoOffsetDateTime() {
        assertEquals("15.05.2026 21:30", formatMonitorTime("2026-05-15T21:30:45+03:00"))
    }

    @Test
    fun formatMonitorLatestScanTime_formatsIsoOffsetDateTime() {
        assertEquals("21:30", formatMonitorLatestScanTime("2026-05-15T21:30:45+03:00"))
    }

    @Test
    fun formatMonitorLatestScanDate_dropsYear() {
        assertEquals("15.05", formatMonitorLatestScanDate("2026-05-15T21:30:45+03:00"))
    }

    @Test
    fun monitorLatestScanCode_usesFallbackWhenMonitorHasNoLatestScan() {
        val snapshot = ScanJobMonitorSnapshot(latestScan = null)

        assertEquals("LOCAL-1", monitorLatestScanCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun monitorLatestScanCode_usesFallbackWhenMonitorCodeIsBlank() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(code = "")
        )

        assertEquals("LOCAL-1", monitorLatestScanCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun monitorLatestScanCode_keepsMonitorCodeWhenLocalCodeDiffers() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(code = "MONITOR-1")
        )

        assertEquals("MONITOR-1", monitorLatestScanCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun directScanResultCode_returnsNullWhenMonitorHasNoCode() {
        val snapshot = ScanJobMonitorSnapshot(latestScan = null)

        assertNull(directScanResultCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun directScanResultCode_returnsNullWhenLocalCodeMatchesMonitorCode() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(code = "SAME-1")
        )

        assertNull(directScanResultCode(snapshot, "SAME-1"))
    }

    @Test
    fun directScanResultCode_returnsLocalCodeWhenItDiffersFromMonitorCode() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(code = "MONITOR-1")
        )

        assertEquals("LOCAL-1", directScanResultCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun monitorLatestScanDisplay_keepsMonitorScanWhenLocalCodeDiffers() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = latestScan(
                code = "MONITOR-1",
                parcelCount = 2,
                boxCount = 1,
                scanSource = ScannedItemSources.BOX_STICKER,
                itemNumbers = listOf("BOX-1")
            )
        )

        val display = monitorLatestScanDisplay(
            snapshot = snapshot,
            lastCode = "LOCAL-1",
            lastParcelCount = 1,
            lastBoxCount = 0,
            lastScanSource = ScannedItemSources.PARCEL_STICKER,
            lastItemNumbers = listOf("PARCEL-1"),
            lastExtData = "local hint",
            lastScanTime = null
        )

        assertEquals("MONITOR-1", display?.code)
        assertEquals(2, display?.parcelCount)
        assertEquals(1, display?.boxCount)
        assertEquals(MonitorLatestScanNumberKind.BOX, display?.numberKind)
        assertEquals(listOf("BOX-1"), display?.itemNumbers)
        assertNull(display?.hint)
    }

    @Test
    fun monitorLatestScanDisplay_parcelScanUsesBackendParcelNumbers() {
        val display = monitorLatestScanDisplay(
            snapshot = null,
            lastCode = "PARCEL-STICKER",
            lastParcelCount = 1,
            lastBoxCount = 0,
            lastScanSource = ScannedItemSources.PARCEL_STICKER,
            lastItemNumbers = listOf("PARCEL-1"),
            lastExtData = null,
            lastScanTime = null
        )

        assertEquals(1, display?.parcelCount)
        assertEquals(0, display?.boxCount)
        assertEquals(MonitorLatestScanNumberKind.PARCEL, display?.numberKind)
        assertEquals(listOf("PARCEL-1"), display?.itemNumbers)
    }

    @Test
    fun monitorLatestScanDisplay_boxScanUsesBackendBoxNumbers() {
        val display = monitorLatestScanDisplay(
            snapshot = null,
            lastCode = "BOX-STICKER",
            lastParcelCount = 3,
            lastBoxCount = 1,
            lastScanSource = ScannedItemSources.BOX_STICKER,
            lastItemNumbers = listOf("BOX-1"),
            lastExtData = null,
            lastScanTime = null
        )

        assertEquals(3, display?.parcelCount)
        assertEquals(1, display?.boxCount)
        assertEquals(MonitorLatestScanNumberKind.BOX, display?.numberKind)
        assertEquals(listOf("BOX-1"), display?.itemNumbers)
    }

    @Test
    fun monitorLatestScanDisplay_emptyItemNumbersHidesNumberLine() {
        val display = monitorLatestScanDisplay(
            snapshot = null,
            lastCode = "UNKNOWN",
            lastParcelCount = 0,
            lastBoxCount = 0,
            lastScanSource = ScannedItemSources.NOT_IN_REGISTER,
            lastItemNumbers = emptyList(),
            lastExtData = null,
            lastScanTime = null
        )

        assertNull(display?.numberKind)
        assertEquals(emptyList<String>(), display?.itemNumbers)
    }

    @Test
    fun checkStatusTextSpec_returnsSpecialStatusText() {
        assertEquals(
            listOf(R.string.check_status_not_checked),
            checkStatusTextSpec(checkStatus(0x0000, 0x0000)).stringResIds
        )
        assertEquals(
            listOf(R.string.check_status_duplicate),
            checkStatusTextSpec(checkStatus(0x017E, 0x017E)).stringResIds
        )
    }

    @Test
    fun checkStatusTextSpec_combinesSwAndFcParts() {
        assertEquals(
            listOf(R.string.check_status_issue_stop_word, R.string.check_status_issue_nonexisting_feacn),
            checkStatusTextSpec(checkStatus(0x0101, 0x0100)).stringResIds
        )
        assertEquals(
            listOf(R.string.check_status_approved, R.string.check_status_fc_no_issues),
            checkStatusTextSpec(checkStatus(0x0010, 0x00A0)).stringResIds
        )
    }

    @Test
    fun checkStatusTextSpec_returnsHexFallbackForUnknownStatus() {
        assertEquals(
            "00010001",
            checkStatusTextSpec(checkStatus(0x0001, 0x0001)).fallbackHex
        )
    }

    @Test
    fun checkStatusTone_matchesUiStatusClasses() {
        assertEquals(CheckStatusTone.NOT_CHECKED, checkStatusTone(checkStatus(0x0000, 0x0000)))
        assertEquals(CheckStatusTone.APPROVED_WITH_EXCISE, checkStatusTone(checkStatus(0x0230, 0x0230)))
        assertEquals(CheckStatusTone.APPROVED_WITH_NOTIFICATION, checkStatusTone(checkStatus(0x0231, 0x0231)))
        assertEquals(CheckStatusTone.APPROVED, checkStatusTone(checkStatus(0x0010, 0x0020)))
        assertEquals(CheckStatusTone.APPROVED_WITH_INHERITANCE, checkStatusTone(checkStatus(0x0010, 0x00A0)))
        assertEquals(CheckStatusTone.HAS_ISSUES_WITH_INHERITANCE, checkStatusTone(checkStatus(0x0010, 0x0180)))
        assertEquals(CheckStatusTone.HAS_ISSUES, checkStatusTone(checkStatus(0x017E, 0x017E)))
        assertEquals(CheckStatusTone.NO_ISSUES, checkStatusTone(checkStatus(0x0010, 0x0010)))
        assertNull(checkStatusTone(null))
    }

    @Test
    fun monitorParcelAttributeSpecs_excludesHiddenFieldsAndKeepsCheckStatus() {
        val parcel = ScanJobMonitorParcel(
            isInRegister = false,
            stickerScanned = true,
            scannedSticker = "SCANNED-1",
            scannedUserName = "User",
            scannedTime = "2026-05-15T21:30:45+03:00",
            parcelId = 123,
            shk = "SHK-1",
            sticker = "ST-1",
            wbSticker = "WB-1",
            sellerSticker = "SELLER-1",
            stickerCode = "CODE-1",
            postingNumber = "POST-1",
            barcode = "BAR-1",
            weightKg = 1.2,
            quantity = 2.0,
            zone = 4,
            zoneName = "Zone A",
            statusId = 9,
            statusTitle = "Ready",
            checkStatus = checkStatus(0x0010, 0x0010)
        )

        val specs = monitorParcelAttributeSpecs(parcel)

        assertEquals(
            listOf(
                R.string.monitor_parcel_scanned_sticker,
                R.string.monitor_parcel_scanned_user,
                R.string.monitor_parcel_scanned_time,
                R.string.monitor_parcel_shk,
                R.string.monitor_parcel_sticker,
                R.string.monitor_parcel_wb_sticker,
                R.string.monitor_parcel_seller_sticker,
                R.string.monitor_parcel_sticker_code,
                R.string.monitor_parcel_posting_number,
                R.string.monitor_parcel_barcode,
                R.string.monitor_parcel_weight_kg,
                R.string.monitor_parcel_quantity,
                R.string.monitor_parcel_zone_name,
                R.string.monitor_parcel_status_title,
                R.string.monitor_parcel_check_status
            ),
            specs.map { it.labelResId }
        )
        assertFalse(specs.any { it.value == "123" })
        assertFalse(specs.any { it.value == "4" })
        assertFalse(specs.any { it.value == "9" })
        assertEquals(checkStatus(0x0010, 0x0010), specs.last().checkStatus)
    }

    private fun checkStatus(fc: Int, sw: Int): Int {
        return (fc shl 16) or sw
    }

    private fun latestScan(
        scanCodeId: Int = 0,
        code: String = "",
        scanTime: String = "",
        area: Int = ScanJobMonitorAreas.BOXES,
        boxId: Int? = null,
        bucketIndex: Int? = null,
        parcelCount: Int = 0,
        boxCount: Int = 0,
        scanSource: Int = ScannedItemSources.NOT_IN_REGISTER,
        itemNumbers: List<String> = emptyList()
    ): ScanJobMonitorLatestScan {
        return ScanJobMonitorLatestScan(
            scanCodeId = scanCodeId,
            code = code,
            scanTime = scanTime,
            parcelCount = parcelCount,
            boxCount = boxCount,
            scanSource = scanSource,
            itemNumbers = itemNumbers,
            area = area,
            boxId = boxId,
            bucketIndex = bucketIndex
        )
    }
}
