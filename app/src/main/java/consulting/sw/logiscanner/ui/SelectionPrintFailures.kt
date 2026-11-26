// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.printer.TajikistanExportSticker

internal data class SelectionPrintKey(val jobId: Int, val parcelId: Int)

internal sealed interface SelectionPrintWork {
    data class Invalid(val error: String) : SelectionPrintWork
    data class Kgt(val code: String) : SelectionPrintWork
    data class Export(val sticker: TajikistanExportSticker) : SelectionPrintWork
}

internal data class SelectionPrintFailure(val jobName: String, val work: SelectionPrintWork, val message: String)

internal fun Map<SelectionPrintKey, SelectionPrintFailure>.afterKgtReprint(
    jobId: Int,
    code: String
): Map<SelectionPrintKey, SelectionPrintFailure> =
    filterNot { (key, failure) -> key.jobId == jobId && failure.work == SelectionPrintWork.Kgt(code) }

internal fun LazyListScope.selectionPrintFailureItems(
    failures: Map<SelectionPrintKey, SelectionPrintFailure>,
    canRetry: Boolean,
    onRetry: (SelectionPrintKey) -> Unit
) {
    items(failures.entries.toList(), key = { "failed-label-${it.key.jobId}-${it.key.parcelId}" }) { (key, failure) ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.parcel_selection_print_item, failure.jobName, key.jobId, key.parcelId),
                style = MaterialTheme.typography.titleSmall
            )
            Text(failure.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (failure.work !is SelectionPrintWork.Invalid) {
                TextButton(onClick = { onRetry(key) }, enabled = canRetry) {
                    Text(stringResource(R.string.parcel_selection_retry_print))
                }
            }
        }
    }
}
