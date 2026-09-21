// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import consulting.sw.logiscanner.net.TajikistanExportStickerPayload

data class TajikistanExportSticker(
    val orderNumber: String,
    val dcBankID: String,
    val placesCount: Int?,
    val dispatchDate: String,
    val weightKg: Double?,
    val costRub: Double?,
    val senderName: String,
    val senderAddress: String,
    val recipientName: String,
    val recipientAddress: String,
    val recipientPhone: String,
    val items: List<TajikistanExportStickerItem>
)

data class TajikistanExportStickerItem(
    val productName: String,
    val quantity: Int?
)

fun TajikistanExportStickerPayload?.toPrintableSticker(): TajikistanExportSticker? {
    val payload = this ?: return null
    val orderNumber = payload.orderNumber.requiredText() ?: return null
    val dcBankID = payload.dcBankID.requiredText() ?: return null
    val placesCount = payload.placesCount?.takeIf { it > 0 } ?: return null
    val dispatchDate = payload.dispatchDate.requiredText() ?: return null
    val weightKg = payload.weightKg?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val costRub = payload.costRub?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val senderName = payload.senderName.requiredText() ?: return null
    val senderAddress = payload.senderAddress.requiredText() ?: return null
    val recipientName = payload.recipientName.requiredText() ?: return null
    val recipientAddress = payload.recipientAddress.requiredText() ?: return null
    val recipientPhone = payload.recipientPhone.requiredText() ?: return null
    val items = payload.items.map { item ->
        TajikistanExportStickerItem(
            productName = item.productName.requiredText() ?: return null,
            quantity = item.quantity?.takeIf { it > 0 } ?: return null
        )
    }.takeIf { it.isNotEmpty() } ?: return null

    return TajikistanExportSticker(
        orderNumber = orderNumber,
        dcBankID = dcBankID,
        placesCount = placesCount,
        dispatchDate = dispatchDate,
        weightKg = weightKg,
        costRub = costRub,
        senderName = senderName,
        senderAddress = senderAddress,
        recipientName = recipientName,
        recipientAddress = recipientAddress,
        recipientPhone = recipientPhone,
        items = items
    )
}

internal fun isSupportedCode128Value(value: String): Boolean {
    return code128SymbolWidthDots(value) != null
}

internal fun code128SymbolWidthDots(value: String): Int? {
    if (value.isEmpty() || value.any { character ->
            character.code !in 0x20..0x7E || character == '"'
        }
    ) {
        return null
    }

    val unreachable = Int.MAX_VALUE / 4
    val codeB = IntArray(value.length + 1) { unreachable }
    val codeC = IntArray(value.length + 1) { unreachable }
    codeB[0] = 0
    codeC[0] = 0
    for (index in value.indices) {
        codeB[index + 1] = minOf(
            codeB[index + 1],
            codeB[index] + 1,
            codeC[index] + 2
        )
        if (index + 1 < value.length && value[index].isDigit() && value[index + 1].isDigit()) {
            codeC[index + 2] = minOf(
                codeC[index + 2],
                codeC[index] + 1,
                codeB[index] + 2
            )
        }
    }

    val dataCodewords = minOf(codeB[value.length], codeC[value.length])
    val symbolWidth = (CODE128_FIXED_MODULES + dataCodewords * CODE128_MODULES_PER_CODEWORD) *
        CODE128_NARROW_DOTS
    val requiredWidth = symbolWidth + 2 * CODE128_QUIET_ZONE_MODULES * CODE128_NARROW_DOTS
    return symbolWidth.takeIf { requiredWidth <= CODE128_AVAILABLE_WIDTH_DOTS }
}

private fun String?.requiredText(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private const val CODE128_AVAILABLE_WIDTH_DOTS = 432
private const val CODE128_NARROW_DOTS = 2
private const val CODE128_FIXED_MODULES = 35
private const val CODE128_MODULES_PER_CODEWORD = 11
private const val CODE128_QUIET_ZONE_MODULES = 10
