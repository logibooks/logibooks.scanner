// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import consulting.sw.logiscanner.BuildConfig
import consulting.sw.logiscanner.R
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
import retrofit2.HttpException

enum class ScanResultColor {
    NONE, YELLOW, GREEN, RED, ORANGE
}

data class MainState(
    val email: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val isBusy: Boolean = false,
    val displayName: String? = null,
    val scanJobs: List<ScanJob> = emptyList(),
    val scanJobTypeDisplays: Map<String, String> = emptyMap(),
    val selectedScanJob: ScanJob? = null,
    val selectedScanJobTypeDisplay: String? = null,
    val isScanning: Boolean = false,
    val lastCode: String? = null,
    val lastCount: Int? = null,
    val lastExtData: String? = null,
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
                    it.copy(displayName = "${loginInfo.firstName} ${loginInfo.lastName}")
                }
            }
        }
    }

    fun setEmail(value: String) = _state.update { it.copy(email = value) }
    fun setPassword(value: String) = _state.update { it.copy(password = value) }

    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            try {
                val url = BuildConfig.SERVER_URL
                loginRepo.login(url, state.value.email, state.value.password)
                
                val token = loginRepo.token
                if (token.isNullOrBlank()) {
                    throw Exception("Login failed: No token received")
                }
                
                // Create repositories with 401 handler that triggers logout
                val unauthorizedHandler: () -> Unit = {
                    viewModelScope.launch {
                        logout()
                    }
                    Unit
                }
                scanJobRepo = ScanJobRepository(url, token, unauthorizedHandler)
                scanRepo = ScanRepository(url, token, unauthorizedHandler)
                loadScanJobs()
                _state.update { it.copy(isLoggedIn = true, password = "") } // Clear password after successful login
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Login failed", ex)
                val errorMessage = when {
                    ex is HttpException && ex.code() == 401 -> 
                        getApplication<Application>().getString(R.string.login_error_invalid_credentials)
                    else -> 
                        getApplication<Application>().getString(R.string.login_error_server_unavailable)
                }
                _state.update { it.copy(error = errorMessage) }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun loadScanJobs() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            try {
                // Fetch operations/mappings first
                scanJobRepo.getOps()
                val jobs = scanJobRepo.getInProgressJobs()
                // Compute type displays for all jobs
                val typeDisplays = jobs.associate { job ->
                    job.type to scanJobRepo.getScanJobTypeDisplay(job.type)
                }
                _state.update { it.copy(scanJobs = jobs, scanJobTypeDisplays = typeDisplays) }
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Failed to load scan jobs", ex)
                _state.update { it.copy(error = ex.message ?: "Unknown error") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun selectScanJob(job: ScanJob?) {
        viewModelScope.launch {
            val typeDisplay = if (job != null && ::scanJobRepo.isInitialized) {
                scanJobRepo.getScanJobTypeDisplay(job.type)
            } else {
                null
            }
            _state.update { 
                it.copy(
                    selectedScanJob = job, 
                    selectedScanJobTypeDisplay = typeDisplay,
                    error = null, 
                    isScanning = false
                ) 
            }
        }
    }

    fun startScanning() {
        _state.update { it.copy(isScanning = true, error = null) }
    }

    fun stopScanning() {
        _state.update { it.copy(isScanning = false) }
    }

    fun onScanned(code: String) {
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
                val result = scanRepo.scan(job.id, code)
                // If hasIssues is true, screen splash shall be orange (overwriting default green/yellow/red scheme)
                val color = if (result.hasIssues) {
                    ScanResultColor.ORANGE
                } else if (result.count == 0) {
                    ScanResultColor.YELLOW
                } else {
                    ScanResultColor.GREEN
                }
                _state.update { 
                    it.copy(
                        lastCode = code, 
                        lastCount = result.count,
                        lastExtData = result.extData,
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
                        lastExtData = null,
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
                MainState(email = it.email, password = "") 
            }
        }
    }
}
