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
    val recipientAddress: String,
    val recipientPhone: String,
    val items: List<TajikistanExportStickerItem>
)

data class TajikistanExportStickerItem(
    val productName: String,
    val quantity: Int?
)

enum class TajikistanStickerField {
    PAYLOAD,
    ORDER_NUMBER,
    PLACES_COUNT,
    DISPATCH_DATE,
    WEIGHT,
    COST,
    SENDER_NAME,
    SENDER_ADDRESS,
    RECIPIENT_ADDRESS,
    RECIPIENT_PHONE,
    ITEMS,
    PRODUCT_NAME,
    QUANTITY
}

enum class TajikistanStickerIssueReason { MISSING, INVALID }

data class TajikistanStickerDataIssue(
    val field: TajikistanStickerField,
    val reason: TajikistanStickerIssueReason,
    val itemNumber: Int? = null
)

data class TajikistanStickerValidationResult(
    val sticker: TajikistanExportSticker?,
    val issues: List<TajikistanStickerDataIssue>
)

fun TajikistanExportStickerPayload?.toPrintableSticker(): TajikistanExportSticker? =
    validateForPrinting().sticker

fun TajikistanExportStickerPayload?.validateForPrinting(): TajikistanStickerValidationResult {
    val payload = this ?: return TajikistanStickerValidationResult(
        sticker = null,
        issues = listOf(
            TajikistanStickerDataIssue(TajikistanStickerField.PAYLOAD, TajikistanStickerIssueReason.MISSING)
        )
    )
    val issues = mutableListOf<TajikistanStickerDataIssue>()

    fun requiredText(value: String?, field: TajikistanStickerField, itemNumber: Int? = null): String? {
        return value.requiredText().also { text ->
            if (text == null) {
                issues += TajikistanStickerDataIssue(field, TajikistanStickerIssueReason.MISSING, itemNumber)
            }
        }
    }

    fun positiveNumber(value: Number?, field: TajikistanStickerField, itemNumber: Int? = null) {
        val reason = when {
            value == null -> TajikistanStickerIssueReason.MISSING
            !value.toDouble().isFinite() || value.toDouble() <= 0.0 -> TajikistanStickerIssueReason.INVALID
            else -> return
        }
        issues += TajikistanStickerDataIssue(field, reason, itemNumber)
    }

    val orderNumber = requiredText(payload.orderNumber, TajikistanStickerField.ORDER_NUMBER)
    val dcBankID = payload.dcBankID.requiredText() ?: DEFAULT_DC_BANK_ID
    positiveNumber(payload.placesCount, TajikistanStickerField.PLACES_COUNT)
    val dispatchDate = requiredText(payload.dispatchDate, TajikistanStickerField.DISPATCH_DATE)
    positiveNumber(payload.weightKg, TajikistanStickerField.WEIGHT)
    positiveNumber(payload.costRub, TajikistanStickerField.COST)
    val senderName = requiredText(payload.senderName, TajikistanStickerField.SENDER_NAME)
    val senderAddress = requiredText(payload.senderAddress, TajikistanStickerField.SENDER_ADDRESS)
    val recipientAddress = requiredText(payload.recipientAddress, TajikistanStickerField.RECIPIENT_ADDRESS)
    val recipientPhone = requiredText(payload.recipientPhone, TajikistanStickerField.RECIPIENT_PHONE)
    if (payload.items.isEmpty()) {
        issues += TajikistanStickerDataIssue(TajikistanStickerField.ITEMS, TajikistanStickerIssueReason.MISSING)
    }
    val items = payload.items.mapIndexedNotNull { index, item ->
        val itemNumber = (index + 1).takeIf { payload.items.size > 1 }
        val productName = requiredText(item.productName, TajikistanStickerField.PRODUCT_NAME, itemNumber)
        positiveNumber(item.quantity, TajikistanStickerField.QUANTITY, itemNumber)
        productName?.let { TajikistanExportStickerItem(it, item.quantity) }
    }
    if (issues.isNotEmpty()) {
        return TajikistanStickerValidationResult(sticker = null, issues = issues)
    }

    val sticker = TajikistanExportSticker(
        orderNumber = requireNotNull(orderNumber),
        dcBankID = dcBankID,
        placesCount = payload.placesCount,
        dispatchDate = requireNotNull(dispatchDate),
        weightKg = payload.weightKg,
        costRub = payload.costRub,
        senderName = requireNotNull(senderName),
        senderAddress = requireNotNull(senderAddress),
        recipientAddress = requireNotNull(recipientAddress),
        recipientPhone = requireNotNull(recipientPhone),
        items = items
    )
    return TajikistanStickerValidationResult(sticker = sticker, issues = emptyList())
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

internal const val DEFAULT_DC_BANK_ID = "DCB_990000000000"
private const val CODE128_AVAILABLE_WIDTH_DOTS = 432
private const val CODE128_NARROW_DOTS = 2
private const val CODE128_FIXED_MODULES = 35
private const val CODE128_MODULES_PER_CODEWORD = 11
private const val CODE128_QUIET_ZONE_MODULES = 10
