// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorFollowTarget
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ParcelCheckStatusProjection
import consulting.sw.logiscanner.net.ParcelCheckStatusProjectionKinds
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScanJobMonitorUiTest {

    @Test
    fun isUnassignedMonitorBox_detectsAreaAndBucket() {
        assertTrue(isUnassignedMonitorBox(ScanJobMonitorBox(area = ScanJobMonitorAreas.UNASSIGNED)))
        assertTrue(isUnassignedMonitorBox(ScanJobMonitorBox(boxId = null, bucketIndex = 2)))
        assertFalse(isUnassignedMonitorBox(ScanJobMonitorBox(area = ScanJobMonitorAreas.BOX, boxId = 7)))
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
    fun isRestrictedMonitorParcel_detectsRestrictionProjection() {
        assertTrue(
            isRestrictedMonitorParcel(
                ScanJobMonitorParcel(
                    checkStatusProjection = ParcelCheckStatusProjection(
                        kind = ParcelCheckStatusProjectionKinds.RESTRICTION,
                        title = "Запрет",
                        restrictionReason = "Стоп-слово"
                    )
                )
            )
        )
        assertTrue(
            isRestrictedMonitorParcel(
                ScanJobMonitorParcel(
                    checkStatusProjection = ParcelCheckStatusProjection(
                        kind = ParcelCheckStatusProjectionKinds.DEFECT,
                        title = "Брак",
                        restrictionReason = "Брак"
                    )
                )
            )
        )
        assertFalse(
            isRestrictedMonitorParcel(
                ScanJobMonitorParcel(
                    checkStatusProjection = ParcelCheckStatusProjection(
                        kind = ParcelCheckStatusProjectionKinds.CHECKED,
                        title = "Проверено",
                        restrictionReason = null
                    )
                )
            )
        )
        assertFalse(isRestrictedMonitorParcel(ScanJobMonitorParcel()))
    }


    @Test
    fun formatMonitorTime_formatsIsoOffsetDateTime() {
        val value = "2026-05-15T21:30:45+03:00"
        val expected = OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"))

        assertEquals(expected, formatMonitorTime(value))
    }

    @Test
    fun formatMonitorTime_convertsUtcToLocalTime() {
        val value = "2026-05-15T18:30:45Z"
        val expected = OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"))

        assertEquals(expected, formatMonitorTime(value))
    }

    @Test
    fun formatMonitorQuantity_dropsFloatingPointForWholeNumbers() {
        assertEquals("2", formatMonitorQuantity(2.0))
        assertEquals("2.5", formatMonitorQuantity(2.5))
    }

    @Test
    fun formatLocalScanResultTime_formatsIsoOffsetDateTime() {
        val value = "2026-05-15T21:30:45+03:00"
        val expected = OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        assertEquals(expected, formatLocalScanResultTime(value))
    }

    @Test
    fun formatLocalScanResultDate_dropsYear() {
        assertEquals("15.05", formatLocalScanResultDate("2026-05-15T21:30:45+03:00"))
    }

    @Test
    fun formatLocalScanResultParts_convertUtcToLocalTime() {
        val value = "2026-05-15T18:30:45Z"
        val local = OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())

        assertEquals(local.format(DateTimeFormatter.ofPattern("HH:mm")), formatLocalScanResultTime(value))
        assertEquals(local.format(DateTimeFormatter.ofPattern("dd.MM")), formatLocalScanResultDate(value))
    }

    @Test
    fun localScanResultDisplay_parcelScanUsesBackendParcelNumbers() {
        val display = localScanResultDisplay(
            lastCode = "PARCEL-STICKER",
            lastParcelCount = 1,
            lastBoxCount = 0,
            lastScanSource = ScannedItemSources.PARCEL_STICKER,
            lastItemNumbers = listOf("PARCEL-1"),
            lastExtData = null,
            lastExtId = null,
            lastScanTime = null
        )

        assertEquals(1, display?.parcelCount)
        assertEquals(0, display?.boxCount)
        assertEquals(LocalScanResultNumberKind.PARCEL, display?.numberKind)
        assertEquals(listOf("PARCEL-1"), display?.itemNumbers)
    }

    @Test
    fun localScanResultDisplay_boxScanUsesBackendBoxNumbers() {
        val display = localScanResultDisplay(
            lastCode = "BOX-STICKER",
            lastParcelCount = 3,
            lastBoxCount = 1,
            lastScanSource = ScannedItemSources.BOX_STICKER,
            lastItemNumbers = listOf("BOX-1"),
            lastExtData = null,
            lastExtId = null,
            lastScanTime = null
        )

        assertEquals(3, display?.parcelCount)
        assertEquals(1, display?.boxCount)
        assertEquals(LocalScanResultNumberKind.BOX, display?.numberKind)
        assertEquals(listOf("BOX-1"), display?.itemNumbers)
    }

    @Test
    fun localScanResultDisplay_emptyItemNumbersHidesNumberLine() {
        val display = localScanResultDisplay(
            lastCode = "UNKNOWN",
            lastParcelCount = 0,
            lastBoxCount = 0,
            lastScanSource = ScannedItemSources.NOT_IN_REGISTER,
            lastItemNumbers = emptyList(),
            lastExtData = null,
            lastExtId = null,
            lastScanTime = null
        )

        assertNull(display?.numberKind)
        assertEquals(emptyList<String>(), display?.itemNumbers)
    }

    @Test
    fun localScanResultDisplay_includesExtIdWhenPresent() {
        val display = localScanResultDisplay(
            lastCode = null,
            lastParcelCount = null,
            lastBoxCount = null,
            lastScanSource = null,
            lastItemNumbers = emptyList(),
            lastExtData = null,
            lastExtId = "15",
            lastScanTime = null
        )

        assertEquals("15", display?.extId)
    }

    @Test
    fun localScanFollowAction_returnsNoneWhenToggleIsOff() {
        val action = localScanFollowAction(
            autoFollowEnabled = false,
            target = ScanJobMonitorFollowTarget(
                area = ScanJobMonitorAreas.BOX,
                boxId = 7,
                parcelId = 99
            )
        )

        assertEquals(LocalScanFollowAction.None, action)
    }

    @Test
    fun localScanFollowAction_opensBoxAndHighlightsParcel() {
        val action = localScanFollowAction(
            autoFollowEnabled = true,
            target = ScanJobMonitorFollowTarget(
                area = ScanJobMonitorAreas.BOX,
                boxId = 7,
                parcelId = 99
            )
        )

        assertEquals(
            LocalScanFollowAction.OpenDetail(
                scope = ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = 7),
                highlightedParcelId = 99
            ),
            action
        )
    }

    @Test
    fun localScanFollowAction_opensUnassignedDefaultBucket() {
        val action = localScanFollowAction(
            autoFollowEnabled = true,
            target = ScanJobMonitorFollowTarget(
                area = ScanJobMonitorAreas.UNASSIGNED,
                parcelId = 101
            )
        )

        assertEquals(
            LocalScanFollowAction.OpenDetail(
                scope = ScanJobMonitorScope(ScanJobMonitorAreas.UNASSIGNED, bucketIndex = 0),
                highlightedParcelId = 101
            ),
            action
        )
    }

    @Test
    fun localScanFollowAction_opensRegisterForNotInRegister() {
        val action = localScanFollowAction(
            autoFollowEnabled = true,
            target = ScanJobMonitorFollowTarget(area = ScanJobMonitorAreas.NOT_IN_REGISTER)
        )

        assertEquals(LocalScanFollowAction.OpenRegister, action)
    }

    @Test
    fun localScanFollowAction_ignoresBoxTargetWithoutBoxId() {
        val action = localScanFollowAction(
            autoFollowEnabled = true,
            target = ScanJobMonitorFollowTarget(area = ScanJobMonitorAreas.BOX)
        )

        assertEquals(LocalScanFollowAction.None, action)
    }

    @Test
    fun localScanFollowAction_returnsNoneWhenTargetIsMissing() {
        val action = localScanFollowAction(
            autoFollowEnabled = true,
            target = ScanJobMonitorFollowTarget()
        )

        assertEquals(LocalScanFollowAction.None, action)
    }
    @Test
    fun monitorParcelAttributeSpecs_excludesHiddenFieldsAndKeepsProjectedCheckStatus() {
        val projection = ParcelCheckStatusProjection(
            kind = ParcelCheckStatusProjectionKinds.RESTRICTION,
            title = "Запрет",
            restrictionReason = "Стоп-слово"
        )
        val parcel = ScanJobMonitorParcel(
            isInRegister = false,
            stickerScanned = true,
            scannedSticker = "SCANNED-1",
            scannedUserName = "User",
            scannedTime = "2026-05-15T21:30:45+03:00",
            parcelId = 123,
            extId = "15",
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
            checkStatusProjection = projection
        )

        val specs = monitorParcelAttributeSpecs(parcel)

        assertEquals(
            listOf(
                R.string.monitor_parcel_scanned_sticker,
                R.string.monitor_parcel_scanned_user,
                R.string.monitor_parcel_scanned_time,
                R.string.monitor_parcel_ext_id,
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
        assertTrue(specs.any { it.labelResId == R.string.monitor_parcel_ext_id && it.value == "15" })
        assertTrue(specs.any { it.labelResId == R.string.monitor_parcel_weight_kg && it.value == "1.200" })
        assertTrue(specs.any { it.labelResId == R.string.monitor_parcel_quantity && it.value == "2" })
        assertEquals(projection, specs.last().checkStatusProjection)
    }

    @Test
    fun monitorParcelAttributeSpecs_appliesCorrectedWeightForRegisteredParcels() {
        val correction = monitorWeightCorrection(
            ScanJobMonitorSnapshot(realWeightKg = 5.0, totalWeightKgToRelease = 10.0)
        )

        val spec = monitorParcelAttributeSpecs(
            ScanJobMonitorParcel(isInRegister = true, weightCorrectionEligible = true, weightKg = 2.4),
            correction
        ).single()

        assertEquals(R.string.monitor_parcel_weight_kg, spec.labelResId)
        assertEquals("2.400", spec.value)
        assertEquals("1.200", spec.correctedValue)
    }

    @Test
    fun monitorParcelAttributeSpecs_keepsPlainWeightWhenCorrectionUnavailableOrNotEligible() {
        assertNull(monitorWeightCorrection(ScanJobMonitorSnapshot(realWeightKg = null, totalWeightKgToRelease = 10.0)))
        assertNull(monitorWeightCorrection(ScanJobMonitorSnapshot(realWeightKg = 5.0, totalWeightKgToRelease = 0.0)))

        val correction = monitorWeightCorrection(
            ScanJobMonitorSnapshot(realWeightKg = 5.0, totalWeightKgToRelease = 10.0)
        )
        val spec = monitorParcelAttributeSpecs(
            ScanJobMonitorParcel(isInRegister = true, weightCorrectionEligible = false, weightKg = 2.4),
            correction
        ).single()

        assertEquals("2.400", spec.value)
        assertNull(spec.correctedValue)
    }

    @Test
    fun monitorParcelAttributeSpecs_usesWbrBarcodeLabelWithoutPostingNumber() {
        val parcel = ScanJobMonitorParcel(
            shk = "SHK-1",
            barcode = "BAR-1"
        )

        val specs = monitorParcelAttributeSpecs(parcel)

        assertTrue(specs.any { it.labelResId == R.string.monitor_parcel_wbr_barcode && it.value == "BAR-1" })
        assertFalse(specs.any { it.labelResId == R.string.monitor_parcel_barcode && it.value == "BAR-1" })
    }

}
