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
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import consulting.sw.logiscanner.net.ParcelCheckStatusProjection
import consulting.sw.logiscanner.net.ParcelCheckStatusProjectionKinds
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.printer.BluetoothPrinterDevice
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import consulting.sw.logiscanner.scan.Mt93ScanReceiver
import consulting.sw.logiscanner.ui.HidScanInput
import consulting.sw.logiscanner.ui.LocalScanResultNumberKind
import consulting.sw.logiscanner.ui.MainViewModel
import consulting.sw.logiscanner.ui.MonitorWeightCorrection
import consulting.sw.logiscanner.ui.ScanResultColor
import consulting.sw.logiscanner.ui.canManualPrintKgtLabel
import consulting.sw.logiscanner.ui.formatLocalScanResultDate
import consulting.sw.logiscanner.ui.formatLocalScanResultTime
import consulting.sw.logiscanner.ui.formatMonitorTime
import consulting.sw.logiscanner.ui.hidFocusRecoverySuspended
import consulting.sw.logiscanner.ui.hidScannerInputEnabled
import consulting.sw.logiscanner.ui.isIssueCheckStatusProjectionKind
import consulting.sw.logiscanner.ui.isRestrictedMonitorParcel
import consulting.sw.logiscanner.ui.isUnassignedMonitorBox
import consulting.sw.logiscanner.ui.bulkyItemsModeEnabled
import consulting.sw.logiscanner.ui.localScanResultDisplay
import consulting.sw.logiscanner.ui.monitorBoxDisplayName
import consulting.sw.logiscanner.ui.monitorParcelAttributeSpecs
import consulting.sw.logiscanner.ui.monitorWeightCorrection
import consulting.sw.logiscanner.ui.parcelPrimaryText
import consulting.sw.logiscanner.ui.scanHintResId
import consulting.sw.logiscanner.ui.theme.LogiScannerTheme
import kotlinx.coroutines.delay
import java.util.Locale

// Focus request retry settings for LoginScreen
private const val MAX_FOCUS_REQUEST_ATTEMPTS = 3
private const val FOCUS_REQUEST_DELAY_MS = 300L

// Light theme check status pill colors
private val CheckStatusRedBackground = Color(0x24F44336)
private val CheckStatusRedText = Color(0xFFB71C1C)
private val CheckStatusRedBorder = Color(0xFFC62828)
private val CheckStatusBlueBackground = Color(0x2E2196F3)
private val CheckStatusBlueText = Color(0xFF0D47A1)
private val CheckStatusBlueBorder = Color(0xFF1565C0)
private val CheckStatusGreenBackground = Color(0x2E4CAF50)
private val CheckStatusGreenText = Color(0xFF1B5E20)
private val CheckStatusGreenBorder = Color(0xFF2E7D32)

// Dark theme check status pill colors (lighter tones for contrast on dark surfaces)
private val CheckStatusDarkRedBackground = Color(0x30EF5350)
private val CheckStatusDarkRedText = Color(0xFFFF8A80)
private val CheckStatusDarkRedBorder = Color(0xFFEF5350)
private val CheckStatusDarkBlueBackground = Color(0x3042A5F5)
private val CheckStatusDarkBlueText = Color(0xFF90CAF9)
private val CheckStatusDarkBlueBorder = Color(0xFF42A5F5)
private val CheckStatusDarkGreenBackground = Color(0x3066BB6A)
private val CheckStatusDarkGreenText = Color(0xFFA5D6A7)
private val CheckStatusDarkGreenBorder = Color(0xFF66BB6A)
private val AutoFollowOnIconColor = Color(0xFF4CAF50)
private val AutoFollowOffIconColor = Color(0xFFFF9800)
private val KgtModeOnIconColor = AutoFollowOnIconColor
private val KgtModeOffIconColor = AutoFollowOffIconColor


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
                                bulkyItemsModeEnabled = bulkyItemsModeEnabled(state.selectedScanJob),
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

            // Register/unregister receiver based on scanning state
            DisposableEffect(state.isScanning) {
                if (state.isScanning) {
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

@Composable
private fun LoginScreen(
    email: String,
    password: String,
    isBusy: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    LaunchedEffect(Unit) {
        // Try a few times to request focus; on some devices the focus target
        // may not be initialized immediately, and requestFocus() can throw
        // IllegalStateException in that case.
        repeat(MAX_FOCUS_REQUEST_ATTEMPTS) { attempt ->
            try {
                delay(FOCUS_REQUEST_DELAY_MS)
                focusRequester.requestFocus()
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(view, 0)
                return@LaunchedEffect
            } catch (_: IllegalStateException) {
                if (attempt == MAX_FOCUS_REQUEST_ATTEMPTS - 1) {
                    // Give up after the last attempt; avoid crashing the app.
                    return@LaunchedEffect
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.app_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.sign_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (BuildConfig.DEBUG) {
                Text(
                    stringResource(R.string.debug_mode_indicator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { 
                            if (it.isFocused) {
                                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showSoftInput(view, 0)
                            }
                        }
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val description = if (passwordVisible) {
                            stringResource(R.string.hide_password)
                        } else {
                            stringResource(R.string.show_password)
                        }
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = description)
                        }
                    },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = onLogin,
                    enabled = !isBusy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (isBusy) stringResource(R.string.logging_in) else stringResource(R.string.login))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        VersionFooter()
    }
}

@Composable
private fun JobSelectionScreen(
    scanJobs: List<ScanJob>,
    scanJobTypeDisplays: Map<String, String>,
    isBusy: Boolean,
    displayName: String?,
    error: String?,
    onSelectJob: (ScanJob) -> Unit,
    onDismissMessage: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
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
                    onClick = onOpenSettings,
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
                    onClick = onRefresh,
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                        contentDescription = stringResource(R.string.refresh_jobs)
                    )
                }
                IconButton(
                    onClick = onLogout,
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
                    stringResource(R.string.select_scan_job),
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

        if (isBusy) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.loading_jobs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (error != null) {
            DismissibleMessage(message = error, onDismiss = onDismissMessage)
        }

        if (scanJobs.isEmpty() && !isBusy) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.no_scan_jobs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scanJobs) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectJob(job) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                job.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!job.description.isNullOrBlank()) {
                                Text(
                                    stringResource(R.string.job_description, job.description),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                stringResource(R.string.job_type, scanJobTypeDisplays[job.type] ?: job.type),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        VersionFooter()
    }
}

@Composable
private fun SettingsScreen(
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
                        .width(40.dp)
                        .height(40.dp)
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
private fun SettingsCheckboxRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DismissibleMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.dismiss_message),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ScanScreen(
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
    bulkyItemsModeEnabled: Boolean,
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
                bulkyItemsModeEnabled = bulkyItemsModeEnabled,
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
private fun PrinterStatusMessages(
    loading: Boolean,
    message: String?,
    error: String?
) {
    if (loading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
    error?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    message?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ScanJobMonitorPanel(
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
    bulkyItemsModeEnabled: Boolean,
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
                if (bulkyItemsModeEnabled) {
                    val kgtModeOn = bulkyItemsMode != BulkyItemsModes.OFF
                    IconButton(
                        onClick = onToggleBulkyItemsMode,
                        modifier = Modifier
                            .width(36.dp)
                            .height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = if (kgtModeOn) {
                                stringResource(R.string.bulky_items_mode_disable)
                            } else {
                                stringResource(R.string.bulky_items_mode_enable)
                            },
                            tint = if (kgtModeOn) {
                                KgtModeOnIconColor
                            } else {
                                KgtModeOffIconColor
                            },
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp)
                        )
                    }
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
            if (bulkyItemsModeEnabled) {
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
            MonitorParcelAttributes(parcel, weightCorrection, onPrintKgtLabel)
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
            } else if (attribute.labelResId == R.string.monitor_parcel_ext_id && canManualPrintKgtLabel(value)) {
                KgtPrintAttribute(
                    label = stringResource(attribute.labelResId),
                    value = value.orEmpty(),
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
private fun CheckStatusPill(
    text: String,
    style: CheckStatusStyle,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = style.content,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(style.background, shape)
            .border(style.borderWidth, style.border, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

private data class CheckStatusStyle(
    val background: Color,
    val content: Color,
    val border: Color,
    val borderWidth: androidx.compose.ui.unit.Dp = 1.dp
)

@Composable
private fun checkStatusStyle(kind: Int?): CheckStatusStyle {
    val darkTheme = isSystemInDarkTheme()
    return when (kind) {
        ParcelCheckStatusProjectionKinds.NOT_CHECKED -> if (darkTheme) CheckStatusStyle(
            CheckStatusDarkBlueBackground,
            CheckStatusDarkBlueText,
            CheckStatusDarkBlueBorder
        ) else CheckStatusStyle(
            CheckStatusBlueBackground,
            CheckStatusBlueText,
            CheckStatusBlueBorder
        )
        ParcelCheckStatusProjectionKinds.RESTRICTION,
        ParcelCheckStatusProjectionKinds.DEFECT -> if (darkTheme) CheckStatusStyle(
            CheckStatusDarkRedBackground,
            CheckStatusDarkRedText,
            CheckStatusDarkRedBorder
        ) else CheckStatusStyle(
            CheckStatusRedBackground,
            CheckStatusRedText,
            CheckStatusRedBorder
        )
        ParcelCheckStatusProjectionKinds.CHECKED -> if (darkTheme) CheckStatusStyle(
            CheckStatusDarkGreenBackground,
            CheckStatusDarkGreenText,
            CheckStatusDarkGreenBorder
        ) else CheckStatusStyle(
            CheckStatusGreenBackground,
            CheckStatusGreenText,
            CheckStatusGreenBorder
        )
        else -> CheckStatusStyle(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline
        )
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
private fun StatusPill(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
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
        display.extId?.takeIf { it.isNotBlank() }?.let { extId ->
            KgtPrintAttribute(
                label = stringResource(R.string.monitor_parcel_ext_id),
                value = extId,
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

@Composable
private fun VersionFooter() {
    Text(
        text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
