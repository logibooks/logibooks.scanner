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
              "latestScan": {
                "scanCodeId": 900,
                "code": "BOX-1",
                "scanTime": "2026-05-15T21:31:00+03:00",
                "parcelCount": 3,
                "boxCount": 1,
                "scanSource": 20,
                "itemNumbers": ["BOX-1"],
                "area": 1,
                "boxId": 7
              },
              "totalBoxes": 2,
              "boxesWithStickerScanned": 1,
              "boxesWithStickerNotScanned": 1,
              "totalParcels": 3,
              "parcelsWithStickerScanned": 2,
              "parcelsWithStickerNotScanned": 1,
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
                  "parcelsWithStickerNotScanned": 1
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
                "parcels": [
                  {
                    "isInRegister": true,
                    "stickerScanned": true,
                    "scannedSticker": "P-1",
                    "scannedUserName": "Operator",
                    "scannedTime": "2026-05-15T21:31:01+03:00",
                    "parcelId": 99,
                    "parcelNumber": "P-1",
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
        assertEquals(900, snapshot.latestScan?.scanCodeId)
        assertEquals(3, snapshot.latestScan?.parcelCount)
        assertEquals(1, snapshot.latestScan?.boxCount)
        assertEquals(ScannedItemSources.BOX_STICKER, snapshot.latestScan?.scanSource)
        assertEquals(listOf("BOX-1"), snapshot.latestScan?.itemNumbers)
        assertEquals(1, snapshot.boxes.size)
        assertTrue(snapshot.boxes.first().boxStickerScanned)
        assertEquals(1, snapshot.box?.parcels?.size)
        assertTrue(snapshot.box!!.parcels!!.first().isInRegister)
        assertEquals(ParcelCheckStatusProjectionKinds.RESTRICTION, snapshot.box!!.parcels!!.first().checkStatusProjection?.kind)
        assertEquals("Запрет", snapshot.box!!.parcels!!.first().checkStatusProjection?.title)
        assertEquals("Стоп-слово", snapshot.box!!.parcels!!.first().checkStatusProjection?.restrictionReason)
    }
}
