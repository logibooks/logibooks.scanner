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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.scan.Mt93ScanReceiver
import consulting.sw.logiscanner.ui.MainViewModel
import consulting.sw.logiscanner.ui.theme.LogiScannerTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var receiver: Mt93ScanReceiver? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by vm.state.collectAsState()

            LogiScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!state.isLoggedIn) {
                        LoginScreen(
                            baseUrl = state.baseUrl,
                            email = state.email,
                            password = state.password,
                            isBusy = state.isBusy,
                            error = state.error,
                            onBaseUrlChange = vm::setBaseUrl,
                            onEmailChange = vm::setEmail,
                            onPasswordChange = vm::setPassword,
                            onLogin = vm::login
                        )
                    } else {
                        ScanScreen(
                            isBusy = state.isBusy,
                            displayName = state.displayName,
                            lastCode = state.lastCode,
                            lastMatch = state.lastMatch,
                            lastBarcodeType = state.lastBarcodeType,
                            error = state.error,
                            onLogout = vm::logout
                        )
                    }
                }
            }

            // Register/unregister receiver based on login state
            DisposableEffect(state.isLoggedIn) {
                if (state.isLoggedIn) {
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
                        Context.RECEIVER_EXPORTED
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
    baseUrl: String,
    email: String,
    password: String,
    isBusy: Boolean,
    error: String?,
    onBaseUrlChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("LogiScanner", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Server Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onLogin,
            enabled = !isBusy && baseUrl.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isBusy) "Logging in..." else "Login")
        }

        Text(
            "Note: baseUrl must include scheme, e.g. https://example.com/ . " +
            "The app will auto-append trailing '/'.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ScanScreen(
    isBusy: Boolean,
    displayName: String?,
    lastCode: String?,
    lastMatch: Boolean?,
    lastBarcodeType: Int?,
    error: String?,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Waiting for scan…", style = MaterialTheme.typography.headlineSmall)
                if (!displayName.isNullOrBlank()) {
                    Text("Logged in as: $displayName", style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = onLogout) { Text("Logout") }
        }

        if (isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (lastCode != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Code: $lastCode")
                    Text("BarcodeType: ${lastBarcodeType ?: -1}")
                    Text(
                        "Server response: " + when (lastMatch) {
                            null -> "(pending)"
                            true -> "MATCH"
                            false -> "NO MATCH"
                        }
                    )
                }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Text(
            "Use the hardware Scan key on the MT93. The app listens to nlscan.action.SCANNER_RESULT.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
