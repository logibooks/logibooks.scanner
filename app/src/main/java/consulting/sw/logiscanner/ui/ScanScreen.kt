// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.repo.ScanJobMonitorScope

@Composable
internal fun ScanScreen(
    isBusy: Boolean,
    displayName: String?,
    selectedJob: ScanJob,
    selectedJobTypeDisplay: String,
    isScanning: Boolean,
    lastCode: String?,
    lastParcelCount: Int?,
    lastBoxCount: Int?,
    lastScanSource: Int?,
    lastItemNumbers: List<String>,
    lastExtData: String?,
    lastExtId: String?,
    lastScanTime: String?,
    externalScannerEnabled: Boolean,
    monitorSnapshot: ScanJobMonitorSnapshot?,
    monitorDetailSnapshot: ScanJobMonitorSnapshot?,
    monitorSelectedScope: ScanJobMonitorScope,
    monitorLoading: Boolean,
    monitorDetailLoading: Boolean,
    monitorError: String?,
    monitorAutoFollow: Boolean,
    bulkyItemsMode: Int,
    relabelingModeAvailable: Boolean,
    printerSelected: Boolean,
    printerLoading: Boolean,
    printerMessage: String?,
    printerError: String?,
    monitorJumpNumber: String,
    monitorJumpLoading: Boolean,
    monitorHighlightedParcelId: Int?,
    error: String?,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onOpenMonitorRegister: () -> Unit,
    onOpenMonitorBox: (ScanJobMonitorBox) -> Unit,
    onToggleMonitorAutoFollow: () -> Unit,
    onToggleBulkyItemsMode: () -> Unit,
    onPrintKgtLabel: (String) -> Unit,
    onMonitorJumpNumberChange: (String) -> Unit,
    onJumpToMonitorNumber: () -> Unit,
    onBackToJobs: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onScanned: (String) -> Unit
) {
    var isJumpFieldFocused by remember { mutableStateOf(false) }
    if (hidScannerInputEnabled(externalScannerEnabled)) {
        HidScanInput(
            enabled = true,
            onScan = onScanned,
            suspendFocusRecovery = hidFocusRecoverySuspended(
                externalScannerEnabled = externalScannerEnabled,
                textFieldFocused = isJumpFieldFocused
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        enabled = !isBusy,
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_open)
                        )
                    }
                    IconButton(
                        onClick = onBackToJobs,
                        enabled = !isBusy,
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_jobs)
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        enabled = !isBusy,
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.logout)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.ready_to_scan),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!displayName.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.logged_in_as, displayName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        selectedJob.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isScanning) {
                                if (isBusy) stringResource(R.string.syncing_with_server)
                                else stringResource(R.string.waiting_for_barcode)
                            } else {
                                stringResource(R.string.scanning_stopped)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (isBusy) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.job_type, selectedJobTypeDisplay),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = if (!isScanning) onStartScanning else onStopScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (!isScanning) stringResource(R.string.start_scanning) else stringResource(R.string.stop_scanning))
                    }
                }
            }
        }

        item {
            ScanJobMonitorPanel(
                snapshot = monitorSnapshot,
                detailSnapshot = monitorDetailSnapshot,
                selectedScope = monitorSelectedScope,
                lastCode = lastCode,
                lastParcelCount = lastParcelCount,
                lastBoxCount = lastBoxCount,
                lastScanSource = lastScanSource,
                lastItemNumbers = lastItemNumbers,
                lastExtData = lastExtData,
                lastExtId = lastExtId,
                lastScanTime = lastScanTime,
                loading = monitorLoading,
                detailLoading = monitorDetailLoading,
                error = monitorError,
                autoFollow = monitorAutoFollow,
                bulkyItemsMode = bulkyItemsMode,
                relabelingModeAvailable = relabelingModeAvailable,
                printerSelected = printerSelected,
                printerLoading = printerLoading,
                printerMessage = printerMessage,
                printerError = printerError,
                onPrintKgtLabel = onPrintKgtLabel,
                jumpNumber = monitorJumpNumber,
                jumpLoading = monitorJumpLoading,
                highlightedParcelId = monitorHighlightedParcelId,
                onOpenRegister = onOpenMonitorRegister,
                onOpenBox = onOpenMonitorBox,
                onToggleAutoFollow = onToggleMonitorAutoFollow,
                onToggleBulkyItemsMode = onToggleBulkyItemsMode,
                onJumpNumberChange = onMonitorJumpNumberChange,
                onJumpToNumber = onJumpToMonitorNumber,
                onJumpFieldFocusChanged = {
                    if (externalScannerEnabled) {
                        isJumpFieldFocused = it
                    }
                }
            )
        }

        if (error != null) {
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Text(
                stringResource(scanHintResId(externalScannerEnabled)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            VersionFooter()
        }
    }
}
