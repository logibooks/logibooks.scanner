// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ParcelScanSelection

internal data class ParcelSelectionState(
    val code: String,
    val bulkyItemsMode: Int,
    val selection: ParcelScanSelection,
    val checkedIds: Set<Int> = selection.candidates.mapTo(mutableSetOf()) { it.parcelId },
    val submitting: Boolean = false,
    val stale: Boolean = false,
    val discardedCodes: List<String> = emptyList()
) {
    val canEdit: Boolean get() = !submitting && !stale
    val showBoxNumbers: Boolean get() = selection.candidates.asSequence()
        .mapNotNull { it.boxNumber?.takeIf(String::isNotBlank) }
        .distinct()
        .drop(1)
        .any()

    fun toggle(id: Int): ParcelSelectionState {
        if (!canEdit || selection.candidates.none { it.parcelId == id }) return this
        return copy(checkedIds = if (id in checkedIds) checkedIds - id else checkedIds + id)
    }
}

internal data class SelectedParcelScan(
    val parcelId: Int,
    val productName: String?,
    val lookupOutcome: String?,
    val extId: String?,
    val exportOrderNumber: String?,
    val message: String? = null,
    val hasIssues: Boolean = false
)

internal fun parcelSelectionSpeech(configuredMessage: String?, prompt: String): String =
    listOfNotNull(configuredMessage?.takeIf(String::isNotBlank), prompt).joinToString(". ")

internal fun isScannerKey(keyCode: Int, unicodeChar: Int): Boolean =
    keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        keyCode == KeyEvent.KEYCODE_TAB || unicodeChar in 0x20..0x7e

@Composable
internal fun ParcelSelectionDialog(
    selection: ParcelSelectionState,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    // Android can deliver HID keyboard events regardless of the configured scan source.
    val suppressKeys = Modifier.focusRequester(focusRequester).onPreviewKeyEvent { event ->
        val key = event.nativeKeyEvent
        isScannerKey(key.keyCode, key.unicodeChar)
    }.focusable()

    AlertDialog(
        modifier = suppressKeys,
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.parcel_selection_title)) },
        text = {
            val choices = selection.selection.candidates
            val showBoxes = selection.showBoxNumbers
            val duplicates = remember(choices, showBoxes) {
                choices.groupingBy {
                    listOf(it.productName, it.boxNumber.takeIf { showBoxes }, it.lookupState, it.sortMessage)
                }.eachCount().filterValues { it > 1 }.keys
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.parcel_selection_code, selection.code))
                if (selection.stale) Text(stringResource(R.string.parcel_selection_stale))
                if (selection.submitting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (!selection.stale) LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    if (selection.discardedCodes.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.parcel_selection_rescan_codes, selection.discardedCodes.joinToString(", ")))
                        }
                    }
                    items(choices, key = { it.parcelId }) { choice ->
                        val checked = choice.parcelId in selection.checkedIds
                        val product = choice.productName?.takeIf(String::isNotBlank)
                        val label = when {
                            product == null -> stringResource(R.string.parcel_selection_unnamed, choice.parcelId)
                            listOf(choice.productName, choice.boxNumber.takeIf { showBoxes }, choice.lookupState,
                                choice.sortMessage) in duplicates -> "$product #${choice.parcelId}"
                            else -> product
                        }
                        val status = when (choice.lookupState) {
                            "outstanding" -> stringResource(R.string.parcel_selection_outstanding)
                            "confirmed" -> stringResource(R.string.parcel_selection_confirmed)
                            "notTarget" -> stringResource(R.string.parcel_selection_not_target)
                            else -> null
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().toggleable(
                                value = checked, enabled = selection.canEdit, role = Role.Checkbox,
                                onValueChange = { onToggle(choice.parcelId) }
                            ).heightIn(min = 48.dp).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, enabled = selection.canEdit)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(listOfNotNull(label, status).joinToString(" · "))
                                if (showBoxes && !choice.boxNumber.isNullOrBlank()) {
                                    Text(
                                        stringResource(R.string.parcel_selection_box, choice.boxNumber),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                choice.sortMessage?.takeIf(String::isNotBlank)?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                selection.stale -> TextButton(onClick = onClose) { Text(stringResource(R.string.parcel_selection_close)) }
                else -> TextButton(onClick = onConfirm, enabled = selection.canEdit && selection.checkedIds.isNotEmpty()) {
                    Text(stringResource(R.string.parcel_selection_confirm, selection.checkedIds.size))
                }
            }
        },
        dismissButton = {
            if (selection.canEdit) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.parcel_selection_skip)) }
            }
        }
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
