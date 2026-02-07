// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import consulting.sw.logiscanner.BuildConfig
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanResultItem
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
import java.util.Locale

enum class ScanResultColor {
    NONE, YELLOW, GREEN, RED, ORANGE
}

fun determineScanResultColor(result: ScanResultItem): ScanResultColor {
    return if (result.hasIssues) {
        ScanResultColor.ORANGE
    } else if (result.count == 0) {
        ScanResultColor.YELLOW
    } else {
        ScanResultColor.GREEN
    }
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
    
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        viewModelScope.launch {
            loginRepo = LoginRepository(getApplication())
            loginRepo.state.collect { loginInfo ->
                _state.update {
                    it.copy(displayName = "${loginInfo.firstName} ${loginInfo.lastName}")
                }
            }
        }
        
        // Initialize TTS with Russian locale and male voice
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val russianLocale = Locale("ru", "RU")
                val result = tts?.setLanguage(russianLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(javaClass.simpleName, "Russian language not supported for TTS")
                    ttsReady = false
                } else {
                    // Try to select a male voice
                    tts?.voices?.find { voice ->
                        voice.locale == russianLocale && voice.name.lowercase().contains("male")
                    }?.let { maleVoice ->
                        tts?.voice = maleVoice
                    }
                    ttsReady = true
                }
            } else {
                Log.e(javaClass.simpleName, "TTS initialization failed")
                ttsReady = false
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
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
                _state.update { 
                    it.copy(
                        lastCode = code, 
                        lastCount = result.count,
                        lastExtData = result.extData,
                        scanResultColor = determineScanResultColor(result)
                    ) 
                }
                
                // Speak extData in parallel with color splash
                if (!result.extData.isNullOrEmpty() && ttsReady) {
                    tts?.speak(result.extData, TextToSpeech.QUEUE_FLUSH, null, "scan_result_${System.currentTimeMillis()}")
                }
                
                // Reset color after a short delay
                colorResetJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(1500)
                    _state.update { it.copy(scanResultColor = ScanResultColor.NONE) }
                }
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Scan failed", ex)
                
                // Check if it's a 400 Bad Request - job selection is no longer valid
                if (ex is HttpException && ex.code() == 400) {
                    // Return to JobSelectionScreen by deselecting the job
                    _state.update { 
                        it.copy(
                            selectedScanJob = null,
                            selectedScanJobTypeDisplay = null,
                            isScanning = false,
                            lastCode = null,
                            lastCount = null,
                            lastExtData = null,
                            error = ex.message ?: "Job selection is no longer valid",
                            scanResultColor = ScanResultColor.NONE
                        )
                    }
                } else {
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
