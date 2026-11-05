// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import com.squareup.moshi.Moshi
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ScanResultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScopedLookupTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun boxAdviceRetainsItsOwnCandidateListAndColor() {
        val result = moshi.adapter(ScanResultItem::class.java).fromJson(
            """{"count":2,"parcelCount":0,"boxCount":1,"scanSource":2,"itemNumbers":["B-1"],
                "extData":"Откройте коробку и проверьте посылки","lookupOutcome":2,
                "candidateParcelNumbers":["P-1","P-2"]}"""
        )!!
        assertEquals(ScanResultColor.INSPECT_BOX, determineScanResultColor(result))
        assertEquals(listOf("B-1"), result.itemNumbers)
        assertEquals(LookupScanFeedback(R.string.lookup_inspect_box, listOf("P-1", "P-2")), lookupScanFeedback(result))
        assertEquals(R.string.lookup_confirmed, lookupScanFeedback(result.copy(lookupOutcome = 1))?.label)
        assertEquals(R.string.lookup_already_confirmed, lookupScanFeedback(result.copy(lookupOutcome = 3))?.label)
        assertNull(lookupScanFeedback(result.copy(lookupOutcome = 0)))
    }

    @Test
    fun countUpdatesRetainOnlyMatchingParcelLists() {
        val adapter = moshi.adapter(ScanJobMonitorSnapshot::class.java)
        val report = adapter.fromJson(
            """{"lookupProgress":{"expectedCount":1,"confirmedCount":0,"missingCount":1,
                "missingParcels":[{"parcelId":1,"parcelNumber":"P-1"}],"confirmedParcels":[]}}"""
        )!!.lookupProgress!!
        val counts = adapter.fromJson(
            """{"lookupProgress":{"expectedCount":1,"confirmedCount":0,"missingCount":1,
                "missingParcels":null,"confirmedParcels":null}}"""
        )!!.lookupProgress!!
        val newerCounts = counts.copy(confirmedCount = 1, missingCount = 0)
        val newerReport = newerCounts.copy(missingParcels = emptyList(), confirmedParcels = report.missingParcels)
        assertEquals(report, mergeLookupProgress(report, counts))
        assertEquals(report, mergeLookupProgress(counts, report))
        assertNull(mergeLookupProgress(report, newerCounts)?.missingParcels)
        assertEquals(newerReport, mergeLookupProgress(counts, newerReport))
        assertEquals(newerReport, mergeLookupProgress(newerReport, counts))
        assertEquals(newerCounts, mergeLookupProgress(newerCounts, report))
    }

    @Test
    fun scopedMonitorProgressDecodesIndependentlyOfRegisterTotals() {
        val snapshot = moshi.adapter(ScanJobMonitorSnapshot::class.java).fromJson(
            """{"lookupTargetMode":1,"operation":30,"totalParcels":99,
                "lookupProgress":{"expectedCount":8,"confirmedCount":2,"missingCount":6,
                    "missingParcels":[{"parcelId":1,"parcelNumber":"P-1"}],
                    "confirmedParcels":[{"parcelId":2,"parcelNumber":"P-2"}]}}"""
        )!!
        assertEquals(R.string.lookup_preset_red, lookupPresetLabel(snapshot.lookupTargetMode, snapshot.operation))
        assertEquals(6, snapshot.lookupProgress?.missingCount)
        assertEquals("P-1", snapshot.lookupProgress?.missingParcels?.single()?.parcelNumber)
        assertEquals("P-2", snapshot.lookupProgress?.confirmedParcels?.single()?.parcelNumber)
        assertEquals(99, snapshot.totalParcels)
        assertEquals(R.string.lookup_preset_missing, lookupPresetLabel(2, 30))
        assertEquals(R.string.lookup_preset_status, lookupPresetLabel(null, 30))
        assertNull(lookupPresetLabel(null, 15))
        val oldJob = moshi.adapter(ScanJob::class.java).fromJson(
            """{"id":1,"name":"Old","description":null,"status":"15","type":"30"}"""
        )!!
        assertNull(oldJob.lookupTargetMode)
    }
}
