// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.printer.BluetoothPrinterDevice

@Composable
internal fun SettingsScreen(
    displayName: String?,
    externalScannerEnabled: Boolean,
    printerBluetoothAddress: String?,
    bondedPrinters: List<BluetoothPrinterDevice>,
    printerAutoPrintEnabled: Boolean,
    kgtVoiceEnabled: Boolean,
    printerLoading: Boolean,
    printerMessage: String?,
    printerError: String?,
    onExternalScannerEnabledChange: (Boolean) -> Unit,
    onPrinterSelected: (String?) -> Unit,
    onPrinterAutoPrintEnabledChange: (Boolean) -> Unit,
    onKgtVoiceEnabledChange: (Boolean) -> Unit,
    onRefreshPrinters: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    onClick = onBack,
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.settings_title),
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_printer_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                PrinterSelectorPanel(
                    printers = bondedPrinters,
                    selectedAddress = printerBluetoothAddress,
                    loading = printerLoading,
                    message = printerMessage,
                    error = printerError,
                    onPrinterSelected = onPrinterSelected,
                    onRefreshPrinters = onRefreshPrinters
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_kgt_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SettingsCheckboxRow(
                    checked = printerAutoPrintEnabled,
                    label = stringResource(R.string.kgt_auto_print_label),
                    onCheckedChange = onPrinterAutoPrintEnabledChange
                )
                SettingsCheckboxRow(
                    checked = kgtVoiceEnabled,
                    label = stringResource(R.string.kgt_voice_enabled_label),
                    onCheckedChange = onKgtVoiceEnabledChange
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_hid_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SettingsCheckboxRow(
                    checked = externalScannerEnabled,
                    label = stringResource(R.string.external_hid_device_label),
                    onCheckedChange = onExternalScannerEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        VersionFooter()
    }
}

@Composable
private fun PrinterSelectorPanel(
    printers: List<BluetoothPrinterDevice>,
    selectedAddress: String?,
    loading: Boolean,
    message: String?,
    error: String?,
    onPrinterSelected: (String?) -> Unit,
    onRefreshPrinters: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selectedPrinterDisplayName(printers, selectedAddress)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = {
                        onRefreshPrinters()
                        expanded = true
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.printer_none)) },
                        onClick = {
                            onPrinterSelected(null)
                            expanded = false
                        }
                    )
                    if (printers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.printer_no_paired)) },
                            enabled = false,
                            onClick = {}
                        )
                    } else {
                        printers.forEach { printer ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        printer.displayName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    onPrinterSelected(printer.address)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onRefreshPrinters,
                enabled = !loading,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.printer_refresh),
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp)
                )
            }
        }

        PrinterStatusMessages(
            loading = loading,
            message = message,
            error = error
        )
    }
}

@Composable
private fun selectedPrinterDisplayName(
    printers: List<BluetoothPrinterDevice>,
    selectedAddress: String?
): String {
    val selectedPrinter = printers.firstOrNull { it.address == selectedAddress }
    return when {
        selectedPrinter != null -> selectedPrinter.displayName
        selectedAddress.isNullOrBlank() -> stringResource(R.string.printer_none)
        else -> stringResource(R.string.printer_selected_address, selectedAddress)
    }
}

@Composable
private fun SettingsCheckboxRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
