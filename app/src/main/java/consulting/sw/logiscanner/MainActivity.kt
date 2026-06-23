// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import consulting.sw.logiscanner.scan.Mt93ScanReceiver
import consulting.sw.logiscanner.ui.JobSelectionScreen
import consulting.sw.logiscanner.ui.LoginScreen
import consulting.sw.logiscanner.ui.MainViewModel
import consulting.sw.logiscanner.ui.ScanResultColor
import consulting.sw.logiscanner.ui.ScanScreen
import consulting.sw.logiscanner.ui.SettingsScreen
import consulting.sw.logiscanner.ui.hasSelectedPrinter
import consulting.sw.logiscanner.ui.relabelingModeAvailable
import consulting.sw.logiscanner.ui.scanReceiverEnabled
import consulting.sw.logiscanner.ui.theme.LogiScannerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var receiver: Mt93ScanReceiver? = null

    companion object {
        // Enforced locale for the application
        private const val APP_LOCALE = "ru"
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.forLanguageTag(APP_LOCALE)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by vm.state.collectAsState()
            val context = LocalContext.current
            val focusManager = LocalFocusManager.current
            val printerSelected = hasSelectedPrinter(state.printerBluetoothAddress)
            var pendingPrinterAction by remember { mutableStateOf<(() -> Unit)?>(null) }
            val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                val action = pendingPrinterAction
                pendingPrinterAction = null
                if (hasBluetoothPrinterPermissions(context)) {
                    action?.invoke()
                } else {
                    vm.setPrinterPermissionDenied()
                }
            }
            val runPrinterAction: (() -> Unit) -> Unit = { action ->
                if (hasBluetoothPrinterPermissions(context)) {
                    action()
                } else {
                    pendingPrinterAction = action
                    bluetoothPermissionLauncher.launch(requiredBluetoothPrinterPermissions())
                }
            }

            LogiScannerTheme {
                // Apply background color based on scan result
                val backgroundColor = when (state.scanResultColor) {
                    ScanResultColor.NOT_FOUND -> colorResource(id = R.color.scan_result_not_found)
                    ScanResultColor.OK -> colorResource(id = R.color.scan_result_ok)
                    ScanResultColor.ISSUE -> colorResource(id = R.color.scan_result_issue)
                    ScanResultColor.SERVER_ERROR -> colorResource(id = R.color.scan_result_server_error)
                    ScanResultColor.IGNORED -> colorResource(id = R.color.scan_result_ignored)
                    ScanResultColor.NONE -> MaterialTheme.colorScheme.background
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    when {
                        !state.isLoggedIn -> {
                            LoginScreen(
                                email = state.email,
                                password = state.password,
                                isBusy = state.isBusy,
                                error = state.error,
                                onEmailChange = vm::setEmail,
                                onPasswordChange = vm::setPassword,
                                onLogin = vm::login
                            )
                        }
                        state.settingsOpen -> {
                            SettingsScreen(
                                displayName = state.displayName,
                                externalScannerEnabled = state.externalScannerEnabled,
                                printerBluetoothAddress = state.printerBluetoothAddress,
                                bondedPrinters = state.bondedPrinters,
                                printerAutoPrintEnabled = state.printerAutoPrintEnabled,
                                kgtVoiceEnabled = state.kgtVoiceEnabled,
                                relabelingSubmode = state.relabelingSubmode,
                                printerLoading = state.printerLoading,
                                printerMessage = state.printerMessage,
                                printerError = state.printerError,
                                onExternalScannerEnabledChange = vm::setExternalScannerEnabled,
                                onPrinterSelected = vm::setPrinterBluetoothAddress,
                                onPrinterAutoPrintEnabledChange = { enabled ->
                                    if (enabled) {
                                        runPrinterAction {
                                            vm.setPrinterAutoPrintEnabled(true)
                                            vm.refreshPrinters()
                                        }
                                    } else {
                                        vm.setPrinterAutoPrintEnabled(false)
                                    }
                                },
                                onKgtVoiceEnabledChange = vm::setKgtVoiceEnabled,
                                onRelabelingSubmodeChange = vm::setRelabelingSubmode,
                                onRefreshPrinters = {
                                    runPrinterAction {
                                        vm.refreshPrinters()
                                    }
                                },
                                onBack = {
                                    if (state.externalScannerEnabled) {
                                        focusManager.clearFocus()
                                    }
                                    vm.closeSettings()
                                }
                            )
                        }
                        state.selectedScanJob == null -> {
                            JobSelectionScreen(
                                scanJobs = state.scanJobs,
                                scanJobTypeDisplays = state.scanJobTypeDisplays,
                                isBusy = state.isBusy,
                                displayName = state.displayName,
                                error = state.error,
                                onSelectJob = vm::selectScanJob,
                                onDismissMessage = vm::dismissMessage,
                                onLogout = {
                                    if (state.externalScannerEnabled) {
                                        focusManager.clearFocus()
                                    }
                                    vm.logout()
                                },
                                onRefresh = vm::loadScanJobs,
                                onOpenSettings = {
                                    if (state.externalScannerEnabled) {
                                        focusManager.clearFocus()
                                    }
                                    vm.openSettings()
                                }
                            )
                        }
                        else -> {
                            ScanScreen(
                                isBusy = state.isBusy,
                                displayName = state.displayName,
                                selectedJob = state.selectedScanJob!!,
                                selectedJobTypeDisplay = state.selectedScanJobTypeDisplay ?: "",
                                isScanning = state.isScanning,
                                lastCode = state.lastCode,
                                lastParcelCount = state.lastParcelCount,
                                lastBoxCount = state.lastBoxCount,
                                lastScanSource = state.lastScanSource,
                                lastItemNumbers = state.lastItemNumbers,
                                lastExtData = state.lastExtData,
                                lastExtId = state.lastExtId,
                                lastScanTime = state.lastScanTime,
                                externalScannerEnabled = state.externalScannerEnabled,
                                monitorSnapshot = state.monitorSnapshot,
                                monitorDetailSnapshot = state.monitorDetailSnapshot,
                                monitorSelectedScope = state.monitorSelectedScope,
                                monitorLoading = state.monitorLoading,
                                monitorDetailLoading = state.monitorDetailLoading,
                                monitorError = state.monitorError,
                                monitorAutoFollow = state.monitorAutoFollow,
                                bulkyItemsMode = state.bulkyItemsMode,
                                relabelingModeAvailable = relabelingModeAvailable(
                                    state.selectedScanJob,
                                    state.relabelingSubmode,
                                    printerSelected
                                ),
                                printerSelected = printerSelected,
                                printerLoading = state.printerLoading,
                                printerMessage = state.printerMessage,
                                printerError = state.printerError,
                                monitorJumpNumber = state.monitorJumpNumber,
                                monitorJumpLoading = state.monitorJumpLoading,
                                monitorHighlightedParcelId = state.monitorHighlightedParcelId,
                                error = state.error,
                                onStartScanning = vm::startScanning,
                                onStopScanning = vm::stopScanning,
                                onOpenMonitorRegister = vm::openMonitorRegister,
                                onOpenMonitorBox = vm::openMonitorBox,
                                onToggleMonitorAutoFollow = vm::toggleMonitorAutoFollow,
                                onToggleBulkyItemsMode = vm::toggleBulkyItemsMode,
                                onPrintKgtLabel = { code ->
                                    runPrinterAction {
                                        vm.printKgtLabel(code)
                                    }
                                },
                                onMonitorJumpNumberChange = vm::setMonitorJumpNumber,
                                onJumpToMonitorNumber = vm::jumpToMonitorNumber,
                                onBackToJobs = {
                                    if (state.externalScannerEnabled) {
                                        focusManager.clearFocus()
                                    }
                                    vm.selectScanJob(null)
                                },
                                onLogout = {
                                    if (state.externalScannerEnabled) {
                                        focusManager.clearFocus()
                                    }
                                    vm.logout()
                                },
                                onOpenSettings = {
                                    if (state.externalScannerEnabled) {
                                        focusManager.clearFocus()
                                    }
                                    vm.openSettings()
                                },
                                onScanned = vm::onScanned
                            )
                        }
                    }
                }
            }

            val mt93ScanReceiverEnabled = scanReceiverEnabled(
                isLoggedIn = state.isLoggedIn,
                settingsOpen = state.settingsOpen,
                selectedJob = state.selectedScanJob
            )

            // Register/unregister receiver based on scan screen visibility.
            DisposableEffect(mt93ScanReceiverEnabled) {
                if (mt93ScanReceiverEnabled) {
                    val r = Mt93ScanReceiver { code ->
                        vm.onScanned(code)
                    }
                    receiver = r
                    registerReceiver(
                        r,
                        IntentFilter("nlscan.action.SCANNER_RESULT"),
                        RECEIVER_EXPORTED
                    )
                } else {
                    receiver?.let { unregisterReceiver(it) }
                    receiver = null
                }

                onDispose {
                    receiver?.let { unregisterReceiver(it) }
                    receiver = null
                }
            }
        }
    }
}

private fun hasBluetoothPrinterPermissions(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        || requiredBluetoothPrinterPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
}

private fun requiredBluetoothPrinterPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        emptyArray()
    }
}
