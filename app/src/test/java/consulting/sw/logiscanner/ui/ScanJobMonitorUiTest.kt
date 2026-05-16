// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorLatestScan
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
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
            latestScan = ScanJobMonitorLatestScan(
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
            latestScan = ScanJobMonitorLatestScan(
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
            latestScan = ScanJobMonitorLatestScan(
                scanCodeId = 10,
                area = ScanJobMonitorAreas.NOT_IN_REGISTER
            )
        )

        assertEquals(ScanJobMonitorScope(ScanJobMonitorAreas.BOXES), latestScanScope(snapshot))
    }

    @Test
    fun latestScanScope_returnsNullWhenBoxIdMissing() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = ScanJobMonitorLatestScan(
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
    fun monitorLatestScanCode_usesFallbackWhenMonitorHasNoLatestScan() {
        val snapshot = ScanJobMonitorSnapshot(latestScan = null)

        assertEquals("LOCAL-1", monitorLatestScanCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun monitorLatestScanCode_usesFallbackWhenMonitorCodeIsBlank() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = ScanJobMonitorLatestScan(code = "")
        )

        assertEquals("LOCAL-1", monitorLatestScanCode(snapshot, "LOCAL-1"))
    }

    @Test
    fun monitorLatestScanCode_keepsMonitorCodeWhenLocalCodeDiffers() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = ScanJobMonitorLatestScan(code = "MONITOR-1")
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
            latestScan = ScanJobMonitorLatestScan(code = "SAME-1")
        )

        assertNull(directScanResultCode(snapshot, "SAME-1"))
    }

    @Test
    fun directScanResultCode_returnsLocalCodeWhenItDiffersFromMonitorCode() {
        val snapshot = ScanJobMonitorSnapshot(
            latestScan = ScanJobMonitorLatestScan(code = "MONITOR-1")
        )

        assertEquals("LOCAL-1", directScanResultCode(snapshot, "LOCAL-1"))
    }
}
