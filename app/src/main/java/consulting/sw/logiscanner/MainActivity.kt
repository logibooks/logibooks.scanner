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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
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
import consulting.sw.logiscanner.scan.Mt93ScanReceiver
import consulting.sw.logiscanner.ui.MainViewModel
import consulting.sw.logiscanner.ui.ScanResultColor
import consulting.sw.logiscanner.ui.HidScanInput
import consulting.sw.logiscanner.ui.theme.LogiScannerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft

// Focus request retry settings for LoginScreen
private const val MAX_FOCUS_REQUEST_ATTEMPTS = 3
private const val FOCUS_REQUEST_DELAY_MS = 300L


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
                                error = state.error,
                                onStartScanning = vm::startScanning,
                                onStopScanning = vm::stopScanning,
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
                // Wait a bit before retrying.
                // Using a small delay to give Compose more time to lay out the focus target.
                // This keeps behavior similar while making it robust on slower devices.
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
    error: String?,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.current_job), style = MaterialTheme.typography.titleMedium)
                Text(
                    selectedJob.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!selectedJob.description.isNullOrBlank()) {
                    Text(
                        selectedJob.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.status), style = MaterialTheme.typography.titleMedium)
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
                    if (isScanning) {
                        if (isBusy) stringResource(R.string.syncing_with_server) 
                        else stringResource(R.string.waiting_for_barcode)
                    } else {
                        stringResource(R.string.scanning_stopped)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.last_scan), style = MaterialTheme.typography.titleMedium)
                if (lastCode != null) {
                    Text(
                        stringResource(R.string.code, lastCode),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (lastCount != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.count_result, lastCount),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!lastExtData.isNullOrBlank()) {
                    Text(
                        text = lastExtData,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.weight(1f))

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