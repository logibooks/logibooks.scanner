// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanJobMonitorModelsTest {

    @Test
    fun scanJobMonitorSnapshot_parsesBackendShape() {
        val json = """
            {
              "scanJobId": 42,
              "scanJobName": "Packing",
              "type": 30,
              "operation": 15,
              "mode": 10,
              "status": 15,
              "registerId": 101,
              "registerType": 1,
              "dealNumber": "D-1",
              "warehouseId": 3,
              "generatedAt": "2026-05-15T21:30:45+03:00",
              "area": 0,
              "totalBoxes": 2,
              "boxesWithStickerScanned": 1,
              "boxesWithStickerNotScanned": 1,
              "totalParcels": 3,
              "parcelsWithStickerScanned": 2,
              "parcelsWithStickerNotScanned": 1,
              "restrictedParcels": 1,
              "scannedItemsNotInRegister": 1,
              "boxes": [
                {
                  "area": 1,
                  "boxId": 7,
                  "boxCode": "BOX-1",
                  "boxStickerScanned": true,
                  "boxScannedSticker": "BOX-1",
                  "boxScannedUserName": "Operator",
                  "boxScannedTime": "2026-05-15T21:31:00+03:00",
                  "totalParcels": 3,
                  "parcelsWithStickerScanned": 2,
                  "parcelsWithStickerNotScanned": 1,
                  "restrictedParcels": 1
                }
              ],
              "box": {
                "area": 1,
                "boxId": 7,
                "boxCode": "BOX-1",
                "boxStickerScanned": true,
                "totalParcels": 1,
                "parcelsWithStickerScanned": 1,
                "parcelsWithStickerNotScanned": 0,
                "restrictedParcels": 1,
                "parcels": [
                  {
                    "isInRegister": true,
                    "stickerScanned": true,
                    "scannedSticker": "P-1",
                    "scannedUserName": "Operator",
                    "scannedTime": "2026-05-15T21:31:01+03:00",
                    "parcelId": 99,
                    "parcelNumber": "P-1",
                    "barcode": "WBR-BAR-1",
                    "zone": 10,
                    "zoneName": "Green",
                    "statusId": 5,
                    "statusTitle": "Ready",
                    "checkStatusProjection": {
                      "kind": 20,
                      "title": "Запрет",
                      "restrictionReason": "Стоп-слово"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val adapter = Moshi.Builder().build().adapter(ScanJobMonitorSnapshot::class.java)
        val snapshot = adapter.fromJson(json)

        assertNotNull(snapshot)
        assertEquals(42, snapshot!!.scanJobId)
        assertEquals(SCAN_JOB_STATUS_IN_PROGRESS, snapshot.status)
        assertEquals(ScanJobMonitorAreas.BOXES, snapshot.area)
        assertEquals(1, snapshot.restrictedParcels)
        assertEquals(1, snapshot.boxes.size)
        assertTrue(snapshot.boxes.first().boxStickerScanned)
        assertEquals(1, snapshot.boxes.first().restrictedParcels)
        assertEquals(1, snapshot.box?.restrictedParcels)
        assertEquals(1, snapshot.box?.parcels?.size)
        assertTrue(snapshot.box!!.parcels!!.first().isInRegister)
        assertEquals("WBR-BAR-1", snapshot.box!!.parcels!!.first().barcode)
        assertEquals(ParcelCheckStatusProjectionKinds.RESTRICTION, snapshot.box!!.parcels!!.first().checkStatusProjection?.kind)
        assertEquals("Запрет", snapshot.box!!.parcels!!.first().checkStatusProjection?.title)
        assertEquals("Стоп-слово", snapshot.box!!.parcels!!.first().checkStatusProjection?.restrictionReason)
    }

    @Test
    fun scanResultItem_parsesFollowTarget() {
        val json = """
            {
              "count": 1,
              "parcelCount": 1,
              "boxCount": 0,
              "scanSource": 10,
              "itemNumbers": ["P-1"],
              "extData": "ok",
              "hasIssues": false,
              "scanCodeId": 900,
              "scanTime": "2026-05-15T21:31:00+03:00",
              "followTarget": {
                "area": 1,
                "boxId": 7,
                "bucketIndex": null,
                "parcelId": 99
              }
            }
        """.trimIndent()

        val adapter = Moshi.Builder().build().adapter(ScanResultItem::class.java)
        val result = adapter.fromJson(json)

        assertNotNull(result)
        assertEquals(900, result!!.scanCodeId)
        assertEquals("2026-05-15T21:31:00+03:00", result.scanTime)
        assertEquals(ScanJobMonitorAreas.BOX, result.followTarget.area)
        assertEquals(7, result.followTarget.boxId)
        assertEquals(99, result.followTarget.parcelId)
        assertEquals(ScannedItemSources.PARCEL_STICKER, result.scanSource)
    }

    @Test
    fun parcelCheckStatusProjection_parsesDefectKind() {
        val json = """
            {
              "kind": 25,
              "title": "Брак",
              "restrictionReason": "Брак"
            }
        """.trimIndent()

        val adapter = Moshi.Builder().build().adapter(ParcelCheckStatusProjection::class.java)
        val projection = adapter.fromJson(json)

        assertNotNull(projection)
        assertEquals(ParcelCheckStatusProjectionKinds.DEFECT, projection!!.kind)
        assertEquals("Брак", projection.title)
        assertEquals("Брак", projection.restrictionReason)
    }

    @Test
    fun scanJobMonitorTarget_parsesParcelTarget() {
        val json = """
            {
              "kind": 2,
              "area": 2,
              "boxId": null,
              "bucketIndex": 1,
              "parcelId": 501,
              "number": "PU-501",
              "boxCode": "Без коробки 2",
              "parcelNumber": "PU-501"
            }
        """.trimIndent()

        val adapter = Moshi.Builder().build().adapter(ScanJobMonitorTarget::class.java)
        val target = adapter.fromJson(json)

        assertNotNull(target)
        assertEquals(ScanJobMonitorTargetKinds.PARCEL, target!!.kind)
        assertEquals(ScanJobMonitorAreas.UNASSIGNED, target.area)
        assertEquals(1, target.bucketIndex)
        assertEquals(501, target.parcelId)
        assertEquals("Без коробки 2", target.boxCode)
        assertEquals("PU-501", target.parcelNumber)
    }
}
