// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.content.Context
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.printer.TajikistanStickerDataIssue
import consulting.sw.logiscanner.printer.TajikistanStickerField
import consulting.sw.logiscanner.printer.TajikistanStickerIssueReason

fun tajikistanStickerDataError(context: Context, issues: List<TajikistanStickerDataIssue>): String {
    if (issues.any { it.field == TajikistanStickerField.PAYLOAD }) {
        return context.getString(R.string.printer_tj_sticker_payload_missing)
    }
    val descriptions = listOfNotNull(
        issues.filter { it.reason == TajikistanStickerIssueReason.MISSING }
            .takeIf { it.isNotEmpty() }
            ?.let { context.getString(R.string.printer_tj_sticker_fields_missing, it.fieldNames(context)) },
        issues.filter { it.reason == TajikistanStickerIssueReason.INVALID }
            .takeIf { it.isNotEmpty() }
            ?.let { context.getString(R.string.printer_tj_sticker_fields_invalid, it.fieldNames(context)) }
    )
    return context.getString(R.string.printer_tj_sticker_missing_data, descriptions.joinToString(" "))
}

private fun List<TajikistanStickerDataIssue>.fieldNames(context: Context): String =
    joinToString(", ") { issue ->
        val fieldName = context.getString(
            when (issue.field) {
                TajikistanStickerField.PAYLOAD -> R.string.printer_tj_sticker_data
                TajikistanStickerField.ORDER_NUMBER -> R.string.printer_tj_sticker_field_order_number
                TajikistanStickerField.PLACES_COUNT -> R.string.printer_tj_sticker_field_places_count
                TajikistanStickerField.DISPATCH_DATE -> R.string.printer_tj_sticker_field_dispatch_date
                TajikistanStickerField.WEIGHT -> R.string.printer_tj_sticker_field_weight
                TajikistanStickerField.COST -> R.string.printer_tj_sticker_field_cost
                TajikistanStickerField.SENDER_NAME -> R.string.printer_tj_sticker_field_sender_name
                TajikistanStickerField.SENDER_ADDRESS -> R.string.printer_tj_sticker_field_sender_address
                TajikistanStickerField.RECIPIENT_NAME -> R.string.printer_tj_sticker_field_recipient_name
                TajikistanStickerField.RECIPIENT_ADDRESS -> R.string.printer_tj_sticker_field_recipient_address
                TajikistanStickerField.RECIPIENT_PHONE -> R.string.printer_tj_sticker_field_recipient_phone
                TajikistanStickerField.ITEMS -> R.string.printer_tj_sticker_field_items
                TajikistanStickerField.PRODUCT_NAME -> R.string.printer_tj_sticker_field_product_name
                TajikistanStickerField.QUANTITY -> R.string.printer_tj_sticker_field_quantity
            }
        )
        if (issue.itemNumber == null) {
            fieldName
        } else {
            context.getString(R.string.printer_tj_sticker_numbered_field, fieldName, issue.itemNumber)
        }
    }
