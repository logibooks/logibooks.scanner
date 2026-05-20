// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Credentials(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class ErrMessage(
    val msg: String
)

@JsonClass(generateAdapter = true)
data class UserViewItemWithJWT(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val email: String,
    val roles: List<String>,
    val token: String
)

@JsonClass(generateAdapter = true)
data class ScanJob(
    val id: Int,
    val name: String,
    val description: String?,
    val status: String,
    val type: String
)

object ScanJobMonitorAreas {
    const val BOXES = 0
    const val BOX = 1
    const val UNASSIGNED = 2
    const val NOT_IN_REGISTER = 3
}

object ScannedItemSources {
    const val UNKNOWN = 0
    const val PARCEL_STICKER = 10
    const val BOX_STICKER = 20
    const val NOT_IN_REGISTER = 30
}

object ParcelCheckStatusProjectionKinds {
    const val NOT_CHECKED = 10
    const val RESTRICTION = 20
    const val DEFECT = 25
    const val CHECKED = 30
}

const val SCAN_JOB_STATUS_IN_PROGRESS = 15

@JsonClass(generateAdapter = true)
data class ScanJobMonitorObserveRequest(
    var scanJobId: Int = 0,
    var area: Int = ScanJobMonitorAreas.BOXES,
    var boxId: Int? = null,
    var bucketIndex: Int? = null
)

@JsonClass(generateAdapter = true)
data class ScanJobMonitorLatestScan(
    var scanCodeId: Int = 0,
    var code: String = "",
    var scanTime: String = "",
    var parcelCount: Int,
    var boxCount: Int,
    var scanSource: Int,
    var itemNumbers: List<String>,
    var area: Int = ScanJobMonitorAreas.BOXES,
    var boxId: Int? = null,
    var bucketIndex: Int? = null
)

@JsonClass(generateAdapter = true)
data class ParcelCheckStatusProjection(
    var kind: Int = 0,
    var title: String = "",
    var restrictionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ScanJobMonitorParcel(
    var isInRegister: Boolean = true,
    var stickerScanned: Boolean = false,
    var scannedSticker: String? = null,
    var scannedUserName: String = "",
    var scannedTime: String? = null,
    var parcelId: Int? = null,
    var parcelNumber: String = "",
    var shk: String? = null,
    var sticker: String? = null,
    var wbSticker: String? = null,
    var sellerSticker: String? = null,
    var stickerCode: String? = null,
    var postingNumber: String? = null,
    var barcode: String? = null,
    var productName: String? = null,
    var weightKg: Double? = null,
    var quantity: Double? = null,
    var zone: Int = 0,
    var zoneName: String = "",
    var statusId: Int = 0,
    var statusTitle: String = "",
    var checkStatusProjection: ParcelCheckStatusProjection? = null
)

@JsonClass(generateAdapter = true)
data class ScanJobMonitorBox(
    var area: Int = ScanJobMonitorAreas.BOX,
    var boxId: Int? = null,
    var bucketIndex: Int? = null,
    var boxCode: String = "",
    var boxStickerScanned: Boolean = false,
    var boxScannedSticker: String? = null,
    var boxScannedUserName: String = "",
    var boxScannedTime: String? = null,
    var totalParcels: Int = 0,
    var parcelsWithStickerScanned: Int = 0,
    var parcelsWithStickerNotScanned: Int = 0,
    var restrictedParcels: Int = 0,
    var parcels: List<ScanJobMonitorParcel>? = null
)

@JsonClass(generateAdapter = true)
data class ScanJobMonitorSnapshot(
    var scanJobId: Int = 0,
    var scanJobName: String = "",
    var type: Int = 0,
    var operation: Int = 0,
    var mode: Int = 0,
    var status: Int = 0,
    var registerId: Int = 0,
    var registerType: Int = 0,
    var dealNumber: String = "",
    var warehouseId: Int = 0,
    var generatedAt: String = "",
    var area: Int = ScanJobMonitorAreas.BOXES,
    var latestScan: ScanJobMonitorLatestScan? = null,
    var totalBoxes: Int = 0,
    var boxesWithStickerScanned: Int = 0,
    var boxesWithStickerNotScanned: Int = 0,
    var totalParcels: Int = 0,
    var parcelsWithStickerScanned: Int = 0,
    var parcelsWithStickerNotScanned: Int = 0,
    var restrictedParcels: Int = 0,
    var scannedItemsNotInRegister: Int = 0,
    var boxes: List<ScanJobMonitorBox> = emptyList(),
    var box: ScanJobMonitorBox? = null
)

@JsonClass(generateAdapter = true)
data class ScanRequest(
    val id: Int,
    val code: String
)

@JsonClass(generateAdapter = true)
data class ScanResultItem(
    val count: Int,
    val parcelCount: Int,
    val boxCount: Int,
    val scanSource: Int,
    val itemNumbers: List<String>,
    val extData: String?,
    val hasIssues: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ScanJobOpsItemDto(
    val value: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class ScanJobOps(
    val types: List<ScanJobOpsItemDto>,
    val operations: List<ScanJobOpsItemDto>,
    val modes: List<ScanJobOpsItemDto>,
    val statuses: List<ScanJobOpsItemDto>
)
