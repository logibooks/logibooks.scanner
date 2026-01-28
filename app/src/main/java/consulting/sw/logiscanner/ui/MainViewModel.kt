// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.repo.LoginRepository
import consulting.sw.logiscanner.repo.ScanJobRepository
import consulting.sw.logiscanner.repo.ScanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanResultColor {
    NONE, YELLOW, GREEN, RED
}

data class MainState(
    val baseUrl: String = "",
    val email: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val isBusy: Boolean = false,
    val displayName: String? = null,
    val scanJobs: List<ScanJob> = emptyList(),
    val selectedScanJob: ScanJob? = null,
    val isScanning: Boolean = false,
    val lastCode: String? = null,
    val lastCount: Int? = null,
    val lastBarcodeType: Int? = null,
    val scanResultColor: ScanResultColor = ScanResultColor.NONE,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private lateinit var loginRepo: LoginRepository
    private lateinit var scanJobRepo: ScanJobRepository
    private lateinit var scanRepo: ScanRepository
    
    private var colorResetJob: Job? = null

    init {
        viewModelScope.launch {
            loginRepo = LoginRepository(getApplication())
            loginRepo.state.collect { loginInfo ->
                _state.update {
                    it.copy(displayName = "${loginInfo.FirstName} ${loginInfo.LastName}")
                }
            }
        }
    }

    fun setBaseUrl(value: String) = _state.update { it.copy(baseUrl = value) }
    fun setEmail(value: String) = _state.update { it.copy(email = value) }
    fun setPassword(value: String) = _state.update { it.copy(password = value) }

    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            try {
                val url = state.value.baseUrl.let { if (!it.endsWith("/")) it.plus("/") else it }
                loginRepo.login(url, state.value.email, state.value.password)
                
                val token = loginRepo.token
                if (token.isNullOrBlank()) {
                    throw Exception("Login failed: No token received")
                }
                
                scanJobRepo = ScanJobRepository(url, token)
                scanRepo = ScanRepository(url, token)
                loadScanJobs()
                _state.update { it.copy(isLoggedIn = true, password = "") } // Clear password after successful login
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Login failed", ex)
                _state.update { it.copy(error = ex.message ?: "Unknown error", password = "") } // Clear password on error
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun loadScanJobs() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            try {
                val jobs = scanJobRepo.getInProgressJobs()
                _state.update { it.copy(scanJobs = jobs) }
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Failed to load scan jobs", ex)
                _state.update { it.copy(error = ex.message ?: "Unknown error") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun selectScanJob(job: ScanJob?) {
        _state.update { it.copy(selectedScanJob = job, error = null, isScanning = false) }
    }

    fun startScanning() {
        _state.update { it.copy(isScanning = true, error = null) }
    }

    fun stopScanning() {
        _state.update { it.copy(isScanning = false) }
    }

    fun onScanned(code: String, barcodeType: Int) {
        val job = state.value.selectedScanJob
        if (job == null) {
            Log.w(javaClass.simpleName, "Scan received but no job selected, ignoring: $code")
            return
        }
        
        // Cancel any pending color reset
        colorResetJob?.cancel()
        
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null, scanResultColor = ScanResultColor.NONE) }
            try {
                val count = scanRepo.scan(job.Id, code)
                val color = if (count == 0) ScanResultColor.YELLOW else ScanResultColor.GREEN
                _state.update { 
                    it.copy(
                        lastCode = code, 
                        lastCount = count, 
                        lastBarcodeType = barcodeType,
                        scanResultColor = color
                    ) 
                }
                // Reset color after a short delay
                colorResetJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(1500)
                    _state.update { it.copy(scanResultColor = ScanResultColor.NONE) }
                }
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Scan failed", ex)
                _state.update { 
                    it.copy(
                        lastCode = code,
                        lastCount = null,
                        lastBarcodeType = barcodeType,
                        error = ex.message ?: "Unknown error",
                        scanResultColor = ScanResultColor.RED
                    )
                }
                // Reset color after a short delay
                colorResetJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(1500)
                    _state.update { it.copy(scanResultColor = ScanResultColor.NONE) }
                }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginRepo.logout()
            colorResetJob?.cancel()
            _state.update { 
                MainState(baseUrl = it.baseUrl, email = it.email, password = "") 
            }
        }
    }
}
