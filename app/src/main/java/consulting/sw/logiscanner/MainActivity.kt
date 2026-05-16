// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorParcel
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.scan.Mt93ScanReceiver
import consulting.sw.logiscanner.ui.MainViewModel
import consulting.sw.logiscanner.ui.ScanResultColor
import consulting.sw.logiscanner.ui.CheckStatusTone
import consulting.sw.logiscanner.ui.HidScanInput
import consulting.sw.logiscanner.ui.LatestScanNumberKind
import consulting.sw.logiscanner.ui.checkStatusText
import consulting.sw.logiscanner.ui.checkStatusTone
import consulting.sw.logiscanner.ui.formatMonitorLatestScanDate
import consulting.sw.logiscanner.ui.formatMonitorLatestScanTime
import consulting.sw.logiscanner.ui.formatMonitorProgress
import consulting.sw.logiscanner.ui.formatMonitorTime
import consulting.sw.logiscanner.ui.isUnassignedMonitorBox
import consulting.sw.logiscanner.ui.latestScanBoxCount
import consulting.sw.logiscanner.ui.latestScanNumberKind
import consulting.sw.logiscanner.ui.monitorBoxDisplayName
import consulting.sw.logiscanner.ui.monitorParcelAttributeSpecs
import consulting.sw.logiscanner.ui.parcelPrimaryText
import consulting.sw.logiscanner.ui.scanJobStatusText
import consulting.sw.logiscanner.ui.theme.LogiScannerTheme
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft

// Focus request retry settings for LoginScreen
private const val MAX_FOCUS_REQUEST_ATTEMPTS = 3
private const val FOCUS_REQUEST_DELAY_MS = 300L

private val CheckStatusRedBackground = Color(0x40F44336)
private val CheckStatusRedText = Color(0xFFD32F2F)
private val CheckStatusRedBorder = Color(0x4DC91104)
private val CheckStatusRedStrongBorder = Color(0xC3C91104)
private val CheckStatusBlueBackground = Color(0x402196F3)
private val CheckStatusBlueText = Color(0xFF1976D2)
private val CheckStatusBlueBorder = Color(0x4D2196F3)
private val CheckStatusGreenBackground = Color(0x404CAF50)
private val CheckStatusGreenText = Color(0xFF388E3C)
private val CheckStatusApprovedText = Color(0xFF2E7D32)
private val CheckStatusGreenBorder = Color(0x4D4CAF50)
private val CheckStatusGreenStrongBorder = Color(0xFF065F0C)
private val CheckStatusOrangeBackground = Color(0x40FF6B35)
private val CheckStatusOrangeText = Color(0xFFD84315)
private val CheckStatusOrangeBorder = Color(0x80FF6B35)
private val CheckStatusPurpleBackground = Color(0x409A35FF)
private val CheckStatusPurpleText = Color(0xFFCE15D8)
private val CheckStatusPurpleBorder = Color(0x809A35FF)


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
            val focusManager = LocalFocusManager.current

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
                        state.selectedScanJob == null -> {
                            JobSelectionScreen(
                                scanJobs = state.scanJobs,
                                scanJobTypeDisplays = state.scanJobTypeDisplays,
                                isBusy = state.isBusy,
                                displayName = state.displayName,
                                error = state.error,
                                onSelectJob = vm::selectScanJob,
                                onLogout = {
                                    focusManager.clearFocus()
                                    vm.logout()
                                },
                                onRefresh = vm::loadScanJobs
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
                                lastCount = state.lastCount,
                                lastExtData = state.lastExtData,
                                lastScanTime = state.lastScanTime,
                                monitorSnapshot = state.monitorSnapshot,
                                monitorDetailSnapshot = state.monitorDetailSnapshot,
                                monitorSelectedScope = state.monitorSelectedScope,
                                monitorLoading = state.monitorLoading,
                                monitorDetailLoading = state.monitorDetailLoading,
                                monitorConnected = state.monitorConnected,
                                monitorClosedStatus = state.monitorClosedStatus,
                                monitorError = state.monitorError,
                                monitorAutoFollow = state.monitorAutoFollow,
                                error = state.error,
                                onStartScanning = vm::startScanning,
                                onStopScanning = vm::stopScanning,
                                onOpenMonitorRegister = vm::openMonitorRegister,
                                onOpenMonitorBox = vm::openMonitorBox,
                                onToggleMonitorAutoFollow = vm::toggleMonitorAutoFollow,
                                onBackToJobs = { 
                                    focusManager.clearFocus()
                                    vm.selectScanJob(null) 
                                },
                                onLogout = {
                                    focusManager.clearFocus()
                                    vm.logout()
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
    val keyboardController = LocalSoftwareKeyboardController.current
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
            } catch (e: IllegalStateException) {
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
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(3f)) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                        contentDescription = stringResource(R.string.refresh_jobs)
                    )
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(R.string.logout)
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
            Text(error, color = MaterialTheme.colorScheme.error)
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
private fun ScanScreen(
    isBusy: Boolean,
    displayName: String?,
    selectedJob: ScanJob,
    selectedJobTypeDisplay: String,
    isScanning: Boolean,
    lastCode: String?,
    lastCount: Int?,
    lastExtData: String?,
    lastScanTime: String?,
    monitorSnapshot: ScanJobMonitorSnapshot?,
    monitorDetailSnapshot: ScanJobMonitorSnapshot?,
    monitorSelectedScope: ScanJobMonitorScope,
    monitorLoading: Boolean,
    monitorDetailLoading: Boolean,
    monitorConnected: Boolean,
    monitorClosedStatus: Int?,
    monitorError: String?,
    monitorAutoFollow: Boolean,
    error: String?,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onOpenMonitorRegister: () -> Unit,
    onOpenMonitorBox: (ScanJobMonitorBox) -> Unit,
    onToggleMonitorAutoFollow: () -> Unit,
    onBackToJobs: () -> Unit,
    onLogout: () -> Unit,
    onScanned: (String) -> Unit
) {
    // HID scan input (Bluetooth keyboard wedge scanners like WD4)
    // Always enabled on scan screen to capture HID input and prevent it from
    // being processed by other UI elements. Actual scan processing is gated
    // by isScanning state in the ViewModel.
    HidScanInput(
        enabled = true,
        onScan = onScanned
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(3f)) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = onBackToJobs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_jobs)
                        )
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.logout)
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
                lastCount = lastCount,
                lastExtData = lastExtData,
                lastScanTime = lastScanTime,
                loading = monitorLoading,
                detailLoading = monitorDetailLoading,
                connected = monitorConnected,
                closedStatus = monitorClosedStatus,
                error = monitorError,
                autoFollow = monitorAutoFollow,
                onOpenRegister = onOpenMonitorRegister,
                onOpenBox = onOpenMonitorBox,
                onToggleAutoFollow = onToggleMonitorAutoFollow
            )
        }

        if (error != null) {
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Text(
                stringResource(R.string.scan_hint),
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
private fun ScanJobMonitorPanel(
    snapshot: ScanJobMonitorSnapshot?,
    detailSnapshot: ScanJobMonitorSnapshot?,
    selectedScope: ScanJobMonitorScope,
    lastCode: String?,
    lastCount: Int?,
    lastExtData: String?,
    lastScanTime: String?,
    loading: Boolean,
    detailLoading: Boolean,
    connected: Boolean,
    closedStatus: Int?,
    error: String?,
    autoFollow: Boolean,
    onOpenRegister: () -> Unit,
    onOpenBox: (ScanJobMonitorBox) -> Unit,
    onToggleAutoFollow: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.monitor_title), style = MaterialTheme.typography.titleMedium)
                StatusPill(
                    text = if (connected) stringResource(R.string.monitor_live) else stringResource(R.string.monitor_offline),
                    background = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (connected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onToggleAutoFollow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (autoFollow) {
                        stringResource(R.string.monitor_auto_follow_disable)
                    } else {
                        stringResource(R.string.monitor_auto_follow_enable)
                    }
                )
            }

            MonitorLatestScanResult(
                snapshot = snapshot,
                lastCode = lastCode,
                lastCount = lastCount,
                lastExtData = lastExtData,
                lastScanTime = lastScanTime
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
                MonitorStat(
                    label = stringResource(R.string.monitor_boxes_progress),
                    value = formatMonitorProgress(
                        snapshot.totalBoxes,
                        snapshot.boxesWithStickerScanned,
                        snapshot.boxesWithStickerNotScanned
                    )
                )
                MonitorStat(
                    label = stringResource(R.string.monitor_parcels_progress),
                    value = formatMonitorProgress(
                        snapshot.totalParcels,
                        snapshot.parcelsWithStickerScanned,
                        snapshot.parcelsWithStickerNotScanned
                    )
                )
                MonitorStat(
                    label = stringResource(R.string.monitor_not_in_register),
                    value = snapshot.scannedItemsNotInRegister.toString()
                )
            }

            if (closedStatus != null) {
                Text(
                    stringResource(R.string.monitor_closed, scanJobStatusText(context, closedStatus)),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            if (snapshot != null) {
                if (selectedScope.area == ScanJobMonitorAreas.BOXES) {
                    MonitorBoxesList(snapshot.boxes, onOpenBox)
                } else {
                    MonitorBoxDetail(
                        snapshot = detailSnapshot,
                        loading = detailLoading,
                        onOpenRegister = onOpenRegister
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
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
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                boxes.forEach { box ->
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
            Text(
                formatMonitorProgress(
                    box.totalParcels,
                    box.parcelsWithStickerScanned,
                    box.parcelsWithStickerNotScanned
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusPill(statusText, statusBackground, statusContent)
    }
}

@Composable
private fun MonitorBoxDetail(
    snapshot: ScanJobMonitorSnapshot?,
    loading: Boolean,
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
        Text(
            boxTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        MonitorStat(
            label = stringResource(R.string.monitor_parcels_progress),
            value = formatMonitorProgress(
                box.totalParcels,
                box.parcelsWithStickerScanned,
                box.parcelsWithStickerNotScanned
            )
        )
        if (!isUnassignedMonitorBox(box)) {
            Text(
                stringResource(
                    R.string.monitor_box_sticker,
                    if (box.boxStickerScanned) stringResource(R.string.monitor_scanned) else stringResource(R.string.monitor_waiting)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (box.boxScannedUserName.isNotBlank()) {
            Text(
                stringResource(R.string.monitor_scan_user, box.boxScannedUserName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!box.boxScannedTime.isNullOrBlank()) {
            Text(
                stringResource(R.string.monitor_scan_time, formatMonitorTime(box.boxScannedTime)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            stringResource(R.string.monitor_parcels),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        val parcels = box.parcels.orEmpty()
        var expandedParcelKey by rememberSaveable(box.boxId, box.bucketIndex, box.boxCode) {
            mutableStateOf<String?>(null)
        }
        if (parcels.isEmpty()) {
            Text(
                stringResource(R.string.monitor_empty_parcels),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = parcels,
                    key = { index, parcel -> parcelExpansionKey(parcel, index) }
                ) { index, parcel ->
                    val parcelKey = parcelExpansionKey(parcel, index)
                    MonitorParcelRow(
                        parcel = parcel,
                        expanded = expandedParcelKey == parcelKey,
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
    expanded: Boolean,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MonitorParcelAttributes(parcel)
        }
    }
}

@Composable
private fun MonitorParcelAttributes(parcel: ScanJobMonitorParcel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        monitorParcelAttributeSpecs(parcel).forEach { attribute ->
            if (attribute.checkStatus != null) {
                MonitorParcelCheckStatusAttribute(attribute.checkStatus)
            } else if (!attribute.value.isNullOrBlank()) {
                MonitorParcelAttribute(stringResource(attribute.labelResId), attribute.value)
            }
        }
    }
}

@Composable
private fun MonitorParcelCheckStatusAttribute(checkStatus: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.monitor_parcel_check_status),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        CheckStatusPill(
            text = checkStatusText(LocalContext.current, checkStatus),
            style = checkStatusStyle(checkStatusTone(checkStatus)),
            modifier = Modifier.weight(0.58f)
        )
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
private fun checkStatusStyle(tone: CheckStatusTone?): CheckStatusStyle {
    return when (tone) {
        CheckStatusTone.NOT_CHECKED -> CheckStatusStyle(
            CheckStatusBlueBackground,
            CheckStatusBlueText,
            CheckStatusBlueBorder
        )
        CheckStatusTone.APPROVED_WITH_EXCISE -> CheckStatusStyle(
            CheckStatusOrangeBackground,
            CheckStatusOrangeText,
            CheckStatusOrangeBorder
        )
        CheckStatusTone.APPROVED_WITH_NOTIFICATION -> CheckStatusStyle(
            CheckStatusPurpleBackground,
            CheckStatusPurpleText,
            CheckStatusPurpleBorder
        )
        CheckStatusTone.HAS_ISSUES_WITH_INHERITANCE -> CheckStatusStyle(
            CheckStatusRedBackground,
            CheckStatusRedText,
            CheckStatusRedStrongBorder,
            3.dp
        )
        CheckStatusTone.HAS_ISSUES -> CheckStatusStyle(
            CheckStatusRedBackground,
            CheckStatusRedText,
            CheckStatusRedBorder
        )
        CheckStatusTone.APPROVED_WITH_INHERITANCE -> CheckStatusStyle(
            CheckStatusGreenBackground,
            CheckStatusApprovedText,
            CheckStatusGreenStrongBorder,
            3.dp
        )
        CheckStatusTone.APPROVED -> CheckStatusStyle(
            CheckStatusGreenBackground,
            CheckStatusApprovedText,
            CheckStatusGreenBorder
        )
        CheckStatusTone.NO_ISSUES -> CheckStatusStyle(
            CheckStatusGreenBackground,
            CheckStatusGreenText,
            CheckStatusGreenBorder
        )
        null -> CheckStatusStyle(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun MonitorParcelAttribute(label: String, value: String) {
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
private fun MonitorLatestScanResult(
    snapshot: ScanJobMonitorSnapshot?,
    lastCode: String?,
    lastCount: Int?,
    lastExtData: String?,
    lastScanTime: String?
) {
    val resultCode = lastCode?.takeIf { it.isNotBlank() }
        ?: snapshot?.latestScan?.code?.takeIf { it.isNotBlank() }
    val displayTimeSource = lastScanTime?.takeIf { it.isNotBlank() }
        ?: snapshot?.latestScan?.scanTime?.takeIf { it.isNotBlank() }
    val displayTime = formatMonitorLatestScanTime(displayTimeSource)
    val displayDate = formatMonitorLatestScanDate(displayTimeSource)
    if (
        resultCode == null
        && lastCount == null
        && lastExtData.isNullOrBlank()
        && displayTime.isBlank()
        && displayDate.isBlank()
    ) {
        return
    }

    val parcelCount = lastCount ?: 0
    val boxCount = latestScanBoxCount(snapshot, lastCount)
    val hint = lastExtData?.takeIf { it.isNotBlank() }
    val countParts = listOfNotNull(
        stringResource(R.string.monitor_latest_scan_parcels, parcelCount),
        stringResource(R.string.monitor_latest_scan_boxes, boxCount),
        hint
    )
    val numberKind = latestScanNumberKind(snapshot, lastCount)
    val numberQuantity = when (numberKind) {
        LatestScanNumberKind.BOX -> boxCount.coerceAtLeast(1)
        LatestScanNumberKind.PARCEL -> parcelCount.coerceAtLeast(1)
    }
    val numberText = resultCode?.let { code ->
        val resources = LocalContext.current.resources
        when (numberKind) {
            LatestScanNumberKind.BOX -> resources.getQuantityString(
                R.plurals.monitor_latest_scan_box_number,
                numberQuantity,
                code
            )
            LatestScanNumberKind.PARCEL -> resources.getQuantityString(
                R.plurals.monitor_latest_scan_parcel_number,
                numberQuantity,
                code
            )
        }
    }
    val title = if (displayTime.isNotBlank() || displayDate.isNotBlank()) {
        stringResource(R.string.monitor_latest_scan_title, displayTime, displayDate).trim()
    } else {
        stringResource(R.string.monitor_latest_scan_title_no_time)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            countParts.joinToString("  "),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (numberText != null) {
            Text(
                text = numberText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
