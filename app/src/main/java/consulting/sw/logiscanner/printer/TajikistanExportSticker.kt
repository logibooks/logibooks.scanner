// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import consulting.sw.logiscanner.net.TajikistanExportStickerPayload

data class TajikistanExportSticker(
    val orderNumber: String,
    val accountNumber: String,
    val placesCount: Int?,
    val dispatchDate: String,
    val weightKg: Double?,
    val declaredValue: Double?,
    val currency: String,
    val senderName: String,
    val senderAddress: String,
    val recipientName: String,
    val recipientAddress: String,
    val recipientPhone: String,
    val items: List<TajikistanExportStickerItem>
)

data class TajikistanExportStickerItem(
    val description: String,
    val quantity: Int?
)

fun TajikistanExportStickerPayload?.toPrintableSticker(): TajikistanExportSticker {
    return TajikistanExportSticker(
        orderNumber = this?.orderNumber.cleanText(),
        accountNumber = this?.accountNumber.cleanText(),
        placesCount = this?.placesCount?.takeIf { it > 0 },
        dispatchDate = this?.dispatchDate.cleanText(),
        weightKg = this?.weightKg?.takeIf { it.isFinite() && it > 0.0 },
        declaredValue = this?.declaredValue?.takeIf { it.isFinite() && it > 0.0 },
        currency = this?.currency.cleanText(),
        senderName = this?.senderName.cleanText(),
        senderAddress = this?.senderAddress.cleanText(),
        recipientName = this?.recipientName.cleanText(),
        recipientAddress = this?.recipientAddress.cleanText(),
        recipientPhone = this?.recipientPhone.cleanText(),
        items = this?.items.orEmpty().map { item ->
            TajikistanExportStickerItem(
                description = item.description.cleanText(),
                quantity = item.quantity?.takeIf { it > 0 }
            )
        }
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

private fun String?.cleanText(): String = this?.trim().orEmpty()

private const val CODE128_AVAILABLE_WIDTH_DOTS = 456
private const val CODE128_NARROW_DOTS = 2
private const val CODE128_FIXED_MODULES = 35
private const val CODE128_MODULES_PER_CODEWORD = 11
private const val CODE128_QUIET_ZONE_MODULES = 10
