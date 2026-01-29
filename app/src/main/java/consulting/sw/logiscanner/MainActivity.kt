// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import java.util.Locale
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.scan.Mt93ScanReceiver
import consulting.sw.logiscanner.ui.MainViewModel
import consulting.sw.logiscanner.ui.ScanResultColor
import consulting.sw.logiscanner.ui.theme.LogiScannerTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var receiver: Mt93ScanReceiver? = null

    companion object {
        // Enforced locale for the application
        private const val APP_LOCALE = "ru"
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale(APP_LOCALE)
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

            LogiScannerTheme {
                // Apply background color based on scan result
                val backgroundColor = when (state.scanResultColor) {
                    ScanResultColor.YELLOW -> Color(0xFFFFEB3B)
                    ScanResultColor.GREEN -> Color(0xFF4CAF50)
                    ScanResultColor.RED -> Color(0xFFF44336)
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
                                isBusy = state.isBusy,
                                displayName = state.displayName,
                                error = state.error,
                                onSelectJob = vm::selectScanJob,
                                onLogout = vm::logout,
                                onRefresh = vm::loadScanJobs
                            )
                        }
                        else -> {
                            ScanScreen(
                                isBusy = state.isBusy,
                                displayName = state.displayName,
                                selectedJob = state.selectedScanJob!!,
                                isScanning = state.isScanning,
                                lastCode = state.lastCode,
                                lastCount = state.lastCount,
                                lastBarcodeType = state.lastBarcodeType,
                                error = state.error,
                                onStartScanning = vm::startScanning,
                                onStopScanning = vm::stopScanning,
                                onBackToJobs = { vm.selectScanJob(null) },
                                onLogout = vm::logout
                            )
                        }
                    }
                }
            }

            // Register/unregister receiver based on scanning state
            DisposableEffect(state.isScanning) {
                if (state.isScanning) {
                    val r = Mt93ScanReceiver { event ->
                        if (event.state == "ok") {
                            val code = event.barcode1?.takeIf { it.isNotBlank() } ?: return@Mt93ScanReceiver
                            vm.onScanned(code, event.barcodeType ?: -1)
                        }
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
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
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
    }
}

@Composable
private fun JobSelectionScreen(
    scanJobs: List<ScanJob>,
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
            Button(onClick = onLogout,
                modifier = Modifier.weight(1.2f)) {
                Text(stringResource(R.string.logout))
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
                    Button(onClick = onRefresh) {
                        Text(stringResource(R.string.refresh_jobs))
                    }
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
                                stringResource(R.string.job_status, job.status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanScreen(
    isBusy: Boolean,
    displayName: String?,
    selectedJob: ScanJob,
    isScanning: Boolean,
    lastCode: String?,
    lastCount: Int?,
    lastBarcodeType: Int?,
    error: String?,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onBackToJobs: () -> Unit,
    onLogout: () -> Unit
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
            Button(onClick = onLogout,
                modifier = Modifier.weight(1.2f)) {
                Text(stringResource(R.string.logout))
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackToJobs,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(stringResource(R.string.back_to_jobs))
                    }
                    if (!isScanning) {
                        Button(
                            onClick = onStartScanning,
                            modifier = Modifier.weight(3f)
                        ) {
                            Text(stringResource(R.string.start_scanning))
                        }
                    } else {
                        Button(
                            onClick = onStopScanning,
                            modifier = Modifier.weight(3f)
                        ) {
                            Text(stringResource(R.string.stop_scanning))
                        }
                    }
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

        if (lastCode != null) {
            val countColor = when {
                lastCount == null -> MaterialTheme.colorScheme.onSurfaceVariant
                lastCount == 0 -> Color(0xFFFFA000) // Amber/Orange for warning
                else -> MaterialTheme.colorScheme.primary
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.last_scan), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.code, lastCode))
                    Text(stringResource(R.string.barcode_type, lastBarcodeType ?: -1))
                    if (lastCount != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.server_response),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.count_result, lastCount),
                                color = countColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
