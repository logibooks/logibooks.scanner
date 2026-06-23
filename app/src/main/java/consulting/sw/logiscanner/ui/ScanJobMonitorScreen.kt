// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.ParcelCheckStatusProjection
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import kotlinx.coroutines.delay

@Composable
internal fun ScanJobMonitorPanel(
    snapshot: ScanJobMonitorSnapshot?,
    detailSnapshot: ScanJobMonitorSnapshot?,
    selectedScope: ScanJobMonitorScope,
    lastCode: String?,
    lastParcelCount: Int?,
    lastBoxCount: Int?,
    lastScanSource: Int?,
    lastItemNumbers: List<String>,
    lastExtData: String?,
    lastExtId: String?,
    lastScanTime: String?,
    loading: Boolean,
    detailLoading: Boolean,
    error: String?,
    autoFollow: Boolean,
    bulkyItemsMode: Int,
    relabelingModeAvailable: Boolean,
    printerSelected: Boolean,
    printerLoading: Boolean,
    printerMessage: String?,
    printerError: String?,
    onPrintKgtLabel: (String) -> Unit,
    jumpNumber: String,
    jumpLoading: Boolean,
    highlightedParcelId: Int?,
    onOpenRegister: () -> Unit,
    onOpenBox: (ScanJobMonitorBox) -> Unit,
    onToggleAutoFollow: () -> Unit,
    onToggleBulkyItemsMode: () -> Unit,
    onJumpNumberChange: (String) -> Unit,
    onJumpToNumber: () -> Unit,
    onJumpFieldFocusChanged: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.monitor_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                val relabelingModeOn = bulkyItemsMode != BulkyItemsModes.OFF
                IconButton(
                    onClick = onToggleBulkyItemsMode,
                    enabled = relabelingModeAvailable,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inventory2,
                        contentDescription = if (relabelingModeOn) {
                            stringResource(R.string.relabeling_mode_disable)
                        } else {
                            stringResource(R.string.relabeling_mode_enable)
                        },
                        tint = when {
                            !relabelingModeAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                            relabelingModeOn -> KgtModeOnIconColor
                            else -> KgtModeOffIconColor
                        },
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleAutoFollow,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = if (autoFollow) {
                            stringResource(R.string.monitor_auto_follow_disable)
                        } else {
                            stringResource(R.string.monitor_auto_follow_enable)
                        },
                        tint = if (autoFollow) {
                            AutoFollowOnIconColor
                        } else {
                            AutoFollowOffIconColor
                        },
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp)
                    )
                }
            }
            if (printerLoading || printerMessage != null || printerError != null || bulkyItemsMode != BulkyItemsModes.OFF) {
                PrinterStatusMessages(
                    loading = printerLoading,
                    message = printerMessage,
                    error = printerError
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = jumpNumber,
                    onValueChange = onJumpNumberChange,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    enabled = !jumpLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onJumpToNumber() }),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .onFocusChanged { onJumpFieldFocusChanged(it.isFocused) },
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (jumpNumber.isBlank()) {
                                    Text(
                                        stringResource(R.string.monitor_jump_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
                IconButton(
                    onClick = onJumpToNumber,
                    enabled = !jumpLoading && jumpNumber.isNotBlank(),
                    modifier = Modifier
                        .width(44.dp)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardDoubleArrowRight,
                        contentDescription = stringResource(R.string.monitor_jump_action),
                        tint = if (!jumpLoading && jumpNumber.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .width(20.dp)
                            .height(20.dp)
                    )
                }
            }
            if (jumpLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            LocalScanResult(
                lastCode = lastCode,
                lastParcelCount = lastParcelCount,
                lastBoxCount = lastBoxCount,
                lastScanSource = lastScanSource,
                lastItemNumbers = lastItemNumbers,
                lastExtData = lastExtData,
                lastExtId = lastExtId,
                lastScanTime = lastScanTime,
                printerSelected = printerSelected,
                onPrintKgtLabel = onPrintKgtLabel
            )

            if (loading && snapshot == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.monitor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            if (snapshot != null && selectedScope.area == ScanJobMonitorAreas.BOXES) {
                MonitorBoxesStatistics(
                    total = snapshot.totalBoxes,
                    scanned = snapshot.boxesWithStickerScanned,
                    notScanned = snapshot.boxesWithStickerNotScanned
                )
                MonitorParcelsStatistics(
                    total = snapshot.totalParcels,
                    scanned = snapshot.parcelsWithStickerScanned,
                    notScanned = snapshot.parcelsWithStickerNotScanned,
                    restricted = snapshot.restrictedParcels
                )
                MonitorAttribute(
                    label = stringResource(R.string.monitor_not_in_register),
                    value = snapshot.scannedItemsNotInRegister.toString()
                )
            }

            if (snapshot != null) {
                if (selectedScope.area == ScanJobMonitorAreas.BOXES) {
                    MonitorBoxesList(snapshot.boxes, onOpenBox)
                } else {
                    MonitorBoxDetail(
                        snapshot = detailSnapshot,
                        loading = detailLoading,
                        highlightedParcelId = highlightedParcelId,
                        printerSelected = printerSelected,
                        onPrintKgtLabel = onPrintKgtLabel,
                        onOpenRegister = onOpenRegister
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorBoxesList(
    boxes: List<ScanJobMonitorBox>,
    onOpenBox: (ScanJobMonitorBox) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.monitor_boxes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (boxes.isEmpty()) {
            Text(
                stringResource(R.string.monitor_empty_boxes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = boxes,
                    key = { box -> "${box.boxId}_${box.bucketIndex}_${box.boxCode}" }
                ) { box ->
                    MonitorBoxRow(box = box, onOpenBox = onOpenBox)
                }
            }
        }
    }
}

@Composable
private fun MonitorBoxRow(
    box: ScanJobMonitorBox,
    onOpenBox: (ScanJobMonitorBox) -> Unit
) {
    val context = LocalContext.current
    val isUnassigned = isUnassignedMonitorBox(box)
    val statusText = when {
        isUnassigned -> stringResource(R.string.monitor_unassigned_group)
        box.boxStickerScanned -> stringResource(R.string.monitor_scanned)
        else -> stringResource(R.string.monitor_waiting)
    }
    val statusBackground = when {
        isUnassigned -> MaterialTheme.colorScheme.secondaryContainer
        box.boxStickerScanned -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusContent = when {
        isUnassigned -> MaterialTheme.colorScheme.onSecondaryContainer
        box.boxStickerScanned -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .clickable { onOpenBox(box) }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                monitorBoxDisplayName(context, box),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MonitorBoxParcelProgressText(box)
        }
        StatusPill(statusText, statusBackground, statusContent)
    }
}

@Composable
private fun MonitorBoxParcelProgressText(box: ScanJobMonitorBox) {
    val restrictedColor = MaterialTheme.colorScheme.error
    val text = buildAnnotatedString {
        append("${box.totalParcels} / ")
        append("${box.parcelsWithStickerScanned} / ")
        append("${box.parcelsWithStickerNotScanned} / ")
        if (box.restrictedParcels > 0) {
            withStyle(SpanStyle(color = restrictedColor, fontWeight = FontWeight.Bold)) {
                append(box.restrictedParcels.toString())
            }
        } else {
            append(box.restrictedParcels.toString())
        }
    }

    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MonitorBoxDetail(
    snapshot: ScanJobMonitorSnapshot?,
    loading: Boolean,
    highlightedParcelId: Int?,
    printerSelected: Boolean,
    onPrintKgtLabel: (String) -> Unit,
    onOpenRegister: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onOpenRegister, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.monitor_back_to_boxes))
        }

        if (loading && snapshot == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.monitor_detail_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val box = snapshot?.box
        if (box == null) {
            Text(
                stringResource(R.string.monitor_empty_parcels),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val boxTitle = if (!isUnassignedMonitorBox(box) && box.boxCode.isNotBlank()) {
            stringResource(R.string.monitor_box_display_name, box.boxCode)
        } else {
            monitorBoxDisplayName(context, box)
        }
        val isUnassigned = isUnassignedMonitorBox(box)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                boxTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!isUnassigned) {
                StatusPill(
                    text = if (box.boxStickerScanned) {
                        stringResource(R.string.monitor_scanned)
                    } else {
                        stringResource(R.string.monitor_waiting)
                    },
                    background = if (box.boxStickerScanned) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (box.boxStickerScanned) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        MonitorParcelsStatistics(
            total = box.totalParcels,
            scanned = box.parcelsWithStickerScanned,
            notScanned = box.parcelsWithStickerNotScanned,
            restricted = box.restrictedParcels
        )
        box.boxScannedSticker?.takeIf { it.isNotBlank() }?.let { scannedSticker ->
            MonitorAttribute(
                label = stringResource(R.string.monitor_parcel_scanned_sticker),
                value = scannedSticker
            )
        }
        if (box.boxScannedUserName.isNotBlank()) {
            MonitorAttribute(
                label = stringResource(R.string.monitor_parcel_scanned_user),
                value = box.boxScannedUserName
            )
        }
        if (!box.boxScannedTime.isNullOrBlank()) {
            MonitorAttribute(
                label = stringResource(R.string.monitor_parcel_scanned_time),
                value = formatMonitorTime(box.boxScannedTime)
            )
        }

        Text(
            stringResource(R.string.monitor_parcels),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        val parcels = box.parcels.orEmpty()
        val weightCorrection = monitorWeightCorrection(snapshot)
        var expandedParcelKey by rememberSaveable(box.boxId, box.bucketIndex, box.boxCode) {
            mutableStateOf<String?>(null)
        }
        val listState = rememberLazyListState()
        LaunchedEffect(highlightedParcelId, parcels) {
            val targetIndex = parcels.indexOfFirst { parcel ->
                isHighlightedMonitorParcel(parcel, highlightedParcelId)
            }
            if (targetIndex >= 0) {
                expandedParcelKey = parcelExpansionKey(parcels[targetIndex], targetIndex)
                listState.animateScrollToItem(targetIndex)
            }
        }
        if (parcels.isEmpty()) {
            Text(
                stringResource(R.string.monitor_empty_parcels),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = parcels,
                    key = { index, parcel -> parcelExpansionKey(parcel, index) }
                ) { index, parcel ->
                    val parcelKey = parcelExpansionKey(parcel, index)
                    val highlighted = isHighlightedMonitorParcel(parcel, highlightedParcelId)
                    MonitorParcelRow(
                        parcel = parcel,
                        weightCorrection = weightCorrection,
                        expanded = expandedParcelKey == parcelKey,
                        highlighted = highlighted,
                        printerSelected = printerSelected,
                        onPrintKgtLabel = onPrintKgtLabel,
                        onToggleExpanded = {
                            expandedParcelKey = if (expandedParcelKey == parcelKey) null else parcelKey
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorParcelRow(
    parcel: ScanJobMonitorParcel,
    weightCorrection: MonitorWeightCorrection?,
    expanded: Boolean,
    highlighted: Boolean,
    printerSelected: Boolean,
    onPrintKgtLabel: (String) -> Unit,
    onToggleExpanded: () -> Unit
) {
    val statusText = when {
        !parcel.isInRegister -> stringResource(R.string.monitor_not_in_register)
        parcel.stickerScanned -> stringResource(R.string.monitor_scanned)
        else -> stringResource(R.string.monitor_waiting)
    }
    val statusBackground = when {
        !parcel.isInRegister -> MaterialTheme.colorScheme.errorContainer
        parcel.stickerScanned -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusContent = when {
        !parcel.isInRegister -> MaterialTheme.colorScheme.onErrorContainer
        parcel.stickerScanned -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rowShape = RoundedCornerShape(6.dp)
    val rowBackground = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val rowModifier = if (highlighted) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, rowShape)
    } else {
        Modifier
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(highlighted, expanded) {
        if (highlighted && expanded) {
            delay(250)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .background(rowBackground, rowShape)
            .then(rowModifier)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                parcelPrimaryText(parcel),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRestrictedMonitorParcel(parcel)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            StatusPill(statusText, statusBackground, statusContent)
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardDoubleArrowUp
                    } else {
                        Icons.Filled.KeyboardDoubleArrowDown
                    },
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.monitor_collapse_parcel
                        } else {
                            R.string.monitor_expand_parcel
                        }
                    ),
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp)
                )
            }
        }

        if (expanded) {
            if (!parcel.productName.isNullOrBlank()) {
                Text(
                    parcel.productName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Visible
                )
            }
            MonitorParcelAttributes(parcel, weightCorrection, printerSelected, onPrintKgtLabel)
        }
    }
}

private fun isHighlightedMonitorParcel(
    parcel: ScanJobMonitorParcel,
    highlightedParcelId: Int?
): Boolean {
    return highlightedParcelId != null && parcel.parcelId == highlightedParcelId
}

@Composable
private fun MonitorParcelAttributes(
    parcel: ScanJobMonitorParcel,
    weightCorrection: MonitorWeightCorrection?,
    printerSelected: Boolean,
    onPrintKgtLabel: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        monitorParcelAttributeSpecs(parcel, weightCorrection).forEach { attribute ->
            val value = attribute.value
            val correctedValue = attribute.correctedValue
            if (attribute.checkStatusProjection != null) {
                MonitorParcelCheckStatusAttribute(attribute.checkStatusProjection)
            } else if (!value.isNullOrBlank() && !correctedValue.isNullOrBlank()) {
                MonitorCorrectedWeightAttribute(
                    label = stringResource(attribute.labelResId),
                    value = value,
                    correctedValue = correctedValue
                )
            } else if (attribute.labelResId == R.string.monitor_parcel_ext_id && kgtLabelCode(value) != null) {
                KgtPrintAttribute(
                    label = stringResource(attribute.labelResId),
                    value = value.orEmpty(),
                    printEnabled = canManualPrintKgtLabel(value, printerSelected),
                    onPrintKgtLabel = onPrintKgtLabel
                )
            } else if (!value.isNullOrBlank()) {
                MonitorAttribute(stringResource(attribute.labelResId), value)
            }
        }
    }
}

@Composable
private fun MonitorCorrectedWeightAttribute(
    label: String,
    value: String,
    correctedValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Row(
            modifier = Modifier.weight(0.58f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(14.dp)
                    .height(14.dp)
            )
            Text(
                correctedValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MonitorParcelCheckStatusAttribute(projection: ParcelCheckStatusProjection) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                stringResource(R.string.monitor_parcel_check_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.42f)
            )
            CheckStatusPill(
                text = projection.title.ifBlank { "-" },
                style = checkStatusStyle(projection.kind),
                modifier = Modifier.weight(0.58f)
            )
        }
        if (isIssueCheckStatusProjectionKind(projection.kind) && !projection.restrictionReason.isNullOrBlank()) {
            MonitorAttribute(
                stringResource(R.string.monitor_parcel_restriction_reason),
                projection.restrictionReason.orEmpty()
            )
        }
    }
}

@Composable
private fun MonitorBoxesStatistics(total: Int, scanned: Int, notScanned: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        MonitorAttribute(
            label = stringResource(R.string.monitor_boxes_total),
            value = total.toString()
        )
        MonitorAttribute(
            label = stringResource(R.string.monitor_stat_scanned),
            value = scanned.toString()
        )
        MonitorAttribute(
            label = stringResource(R.string.monitor_stat_not_scanned),
            value = notScanned.toString()
        )
    }
}

@Composable
private fun MonitorParcelsStatistics(total: Int, scanned: Int, notScanned: Int, restricted: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        MonitorAttribute(
            label = stringResource(R.string.monitor_parcels_total),
            value = total.toString()
        )
        MonitorAttribute(
            label = stringResource(R.string.monitor_stat_scanned),
            value = scanned.toString()
        )
        MonitorAttribute(
            label = stringResource(R.string.monitor_stat_not_scanned),
            value = notScanned.toString()
        )
        MonitorAttribute(
            label = stringResource(R.string.monitor_restricted),
            value = restricted.toString()
        )
    }
}

@Composable
private fun MonitorAttribute(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun KgtPrintAttribute(
    label: String,
    value: String,
    printEnabled: Boolean,
    onPrintKgtLabel: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Row(
            modifier = Modifier.weight(0.58f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onPrintKgtLabel(value) },
                enabled = printEnabled,
                modifier = Modifier
                    .width(32.dp)
                    .height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Print,
                    contentDescription = stringResource(R.string.printer_print_label),
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp)
                )
            }
        }
    }
}

private fun parcelExpansionKey(parcel: ScanJobMonitorParcel, index: Int): String {
    return parcel.parcelId?.let { "id:$it" }
        ?: parcel.parcelNumber.takeIf { it.isNotBlank() }?.let { "parcel:$it" }
        ?: parcel.postingNumber?.takeIf { it.isNotBlank() }?.let { "posting:$it" }
        ?: parcel.barcode?.takeIf { it.isNotBlank() }?.let { "barcode:$it" }
        ?: "index:$index"
}

@Composable
private fun LocalScanResult(
    lastCode: String?,
    lastParcelCount: Int?,
    lastBoxCount: Int?,
    lastScanSource: Int?,
    lastItemNumbers: List<String>,
    lastExtData: String?,
    lastExtId: String?,
    lastScanTime: String?,
    printerSelected: Boolean,
    onPrintKgtLabel: (String) -> Unit
) {
    val display = localScanResultDisplay(
        lastCode = lastCode,
        lastParcelCount = lastParcelCount,
        lastBoxCount = lastBoxCount,
        lastScanSource = lastScanSource,
        lastItemNumbers = lastItemNumbers,
        lastExtData = lastExtData,
        lastExtId = lastExtId,
        lastScanTime = lastScanTime
    ) ?: return
    val displayTime = formatLocalScanResultTime(display.scanTime)
    val displayDate = formatLocalScanResultDate(display.scanTime)
    val stickerCode = display.code?.takeIf { it.isNotBlank() }
    val numberAttribute = display.numberKind?.let { numberKind ->
        val resources = LocalResources.current
        val value = display.itemNumbers.joinToString(", ")
        when (numberKind) {
            LocalScanResultNumberKind.BOX -> Pair(
                resources.getQuantityString(
                    R.plurals.monitor_local_scan_result_box_number_label,
                    display.itemNumbers.size
                ),
                value
            )
            LocalScanResultNumberKind.PARCEL -> Pair(
                resources.getQuantityString(
                    R.plurals.monitor_local_scan_result_parcel_number_label,
                    display.itemNumbers.size
                ),
                value
            )
        }
    }
    val title = if (displayTime.isNotBlank() || displayDate.isNotBlank()) {
        stringResource(R.string.monitor_local_scan_result_title, displayTime, displayDate).trim()
    } else {
        stringResource(R.string.monitor_local_scan_result_title_no_time)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stringResource(R.string.monitor_local_scan_result_parcels, display.parcelCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.42f)
            )
            Text(
                text = stringResource(R.string.monitor_local_scan_result_boxes, display.boxCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.58f)
            )
        }
        display.hint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        display.extId?.takeIf { kgtLabelCode(it) != null }?.let { extId ->
            KgtPrintAttribute(
                label = stringResource(R.string.monitor_parcel_ext_id),
                value = extId,
                printEnabled = canManualPrintKgtLabel(extId, printerSelected),
                onPrintKgtLabel = onPrintKgtLabel
            )
        }
        stickerCode?.let { code ->
            MonitorAttribute(
                label = stringResource(R.string.monitor_local_scan_result_sticker_label),
                value = code
            )
        }
        numberAttribute?.let { (label, value) ->
            MonitorAttribute(label = label, value = value)
        }
    }
}
