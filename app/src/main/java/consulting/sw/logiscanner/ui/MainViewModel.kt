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
import consulting.sw.logiscanner.net.SCAN_JOB_STATUS_IN_PROGRESS
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ScanJobMonitorTargetKinds
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.repo.LoginRepository
import consulting.sw.logiscanner.repo.ScanJobMonitorRepository
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import consulting.sw.logiscanner.repo.ScanJobRepository
import consulting.sw.logiscanner.repo.ScanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.OffsetDateTime
import java.util.Locale

enum class ScanResultColor {
    NONE, NOT_FOUND, OK, ISSUE, SERVER_ERROR
}

fun determineScanResultColor(result: ScanResultItem): ScanResultColor {
    return if (result.hasIssues) {
        ScanResultColor.ISSUE
    } else if (result.count == 0) {
        ScanResultColor.NOT_FOUND
    } else {
        ScanResultColor.OK
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
    val monitorSnapshot: ScanJobMonitorSnapshot? = null,
    val monitorDetailSnapshot: ScanJobMonitorSnapshot? = null,
    val monitorSelectedScope: ScanJobMonitorScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES),
    val monitorLoading: Boolean = false,
    val monitorDetailLoading: Boolean = false,
    val monitorError: String? = null,
    val monitorAutoFollow: Boolean = true,
    val monitorJumpNumber: String = "",
    val monitorJumpLoading: Boolean = false,
    val monitorHighlightedParcelId: Int? = null,
    val isScanning: Boolean = false,
    val lastCode: String? = null,
    val lastParcelCount: Int? = null,
    val lastBoxCount: Int? = null,
    val lastScanSource: Int? = null,
    val lastItemNumbers: List<String> = emptyList(),
    val lastExtData: String? = null,
    val lastScanTime: String? = null,
    val scanResultColor: ScanResultColor = ScanResultColor.NONE,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private lateinit var loginRepo: LoginRepository
    private lateinit var scanJobRepo: ScanJobRepository
    private lateinit var scanJobMonitorRepo: ScanJobMonitorRepository
    private lateinit var scanRepo: ScanRepository
    
    private var colorResetJob: Job? = null
    private var monitorJob: Job? = null
    private var monitorDetailJob: Job? = null
    private var scanJobListMonitorJob: Job? = null
    private var scanJobListRefreshJob: Job? = null
    private var scanJobListMonitorVersion = 0
    private var monitorScopeVersion = 0
    private var monitorLatestScanCodeId: Int? = null
    
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
                val russianLocale = Locale.forLanguageTag("ru-RU")
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
        monitorJob?.cancel()
        monitorDetailJob?.cancel()
        scanJobListMonitorJob?.cancel()
        scanJobListRefreshJob?.cancel()
        if (::scanJobMonitorRepo.isInitialized) {
            scanJobMonitorRepo.closeInBackground()
        }
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
                    // Log technical error
                    Log.e(javaClass.simpleName, "Login failed: No token received")
                    // Set user-facing error message
                    _state.update { it.copy(
                        error = getApplication<Application>().getString(R.string.login_error_no_token),
                        isBusy = false
                    ) }
                    return@launch
                }
                
                // Create repositories with 401 handler that triggers logout
                val unauthorizedHandler: () -> Unit = {
                    viewModelScope.launch {
                        logout()
                    }
                }
                scanJobRepo = ScanJobRepository(url, token, unauthorizedHandler)
                scanJobMonitorRepo = ScanJobMonitorRepository(url, token, unauthorizedHandler)
                scanRepo = ScanRepository(url, token, unauthorizedHandler)
                loadScanJobs()
                startScanJobListMonitor()
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
            loadScanJobsInternal(clearError = true)
        }
    }

    private suspend fun loadScanJobsInternal(clearError: Boolean): Boolean {
        if (!::scanJobRepo.isInitialized) {
            return false
        }

        _state.update {
            if (clearError) {
                it.copy(isBusy = true, error = null)
            } else {
                it.copy(isBusy = true)
            }
        }
        try {
            // Fetch operations/mappings first
            scanJobRepo.getOps()
            val jobs = scanJobRepo.getInProgressJobs()
            // Compute type displays for all jobs
            val typeDisplays = jobs.associate { job ->
                job.type to scanJobRepo.getScanJobTypeDisplay(job.type)
            }
            _state.update { it.copy(scanJobs = jobs, scanJobTypeDisplays = typeDisplays) }
            return true
        } catch (ex: Exception) {
            Log.e(javaClass.simpleName, "Failed to load scan jobs: ${ex.message}", ex)
            if (clearError) {
                _state.update { it.copy(error = getApplication<Application>().getString(R.string.error_unknown)) }
            }
            return false
        } finally {
            _state.update { it.copy(isBusy = false) }
        }
    }

    private fun startScanJobListMonitor() {
        if (!::scanJobMonitorRepo.isInitialized) {
            return
        }

        scanJobListMonitorVersion += 1
        val version = scanJobListMonitorVersion
        scanJobListMonitorJob?.cancel()
        scanJobListMonitorJob = viewModelScope.launch {
            try {
                scanJobMonitorRepo.stopScanJobs()
                scanJobMonitorRepo.observeScanJobs(
                    onChanged = {
                        viewModelScope.launch {
                            scheduleScanJobListRefresh(version)
                        }
                    },
                    onConnectionClosed = { exception ->
                        if (exception != null && version == scanJobListMonitorVersion) {
                            Log.e(javaClass.simpleName, "Scan jobs list connection closed", exception)
                        }
                    }
                )
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Log.e(javaClass.simpleName, "Failed to start scan jobs list monitor", ex)
            }
        }
    }

    private fun scheduleScanJobListRefresh(version: Int) {
        if (version != scanJobListMonitorVersion || state.value.selectedScanJob != null) {
            return
        }

        scanJobListRefreshJob?.cancel()
        scanJobListRefreshJob = viewModelScope.launch {
            delay(SCAN_JOB_LIST_REFRESH_DEBOUNCE_MS)
            if (version == scanJobListMonitorVersion && state.value.selectedScanJob == null) {
                refreshScanJobsKeepingMessage()
            }
        }
    }

    fun selectScanJob(job: ScanJob?) {
        viewModelScope.launch {
            val jobChanged = state.value.selectedScanJob?.id != job?.id
            if (job == null) {
                stopScanJobMonitor()
            }
            val typeDisplay = if (job != null && ::scanJobRepo.isInitialized) {
                scanJobRepo.getScanJobTypeDisplay(job.type)
            } else {
                null
            }
            _state.update { 
                it.copy(
                    selectedScanJob = job, 
                    selectedScanJobTypeDisplay = typeDisplay,
                    monitorSnapshot = if (job == null) null else it.monitorSnapshot,
                    monitorDetailSnapshot = if (job == null) null else it.monitorDetailSnapshot,
                    monitorSelectedScope = if (job == null) {
                        ScanJobMonitorScope(ScanJobMonitorAreas.BOXES)
                    } else {
                        it.monitorSelectedScope
                    },
                    monitorLoading = if (job == null) false else it.monitorLoading,
                    monitorDetailLoading = if (job == null) false else it.monitorDetailLoading,
                    monitorError = if (job == null) null else it.monitorError,
                    monitorJumpNumber = if (jobChanged) "" else it.monitorJumpNumber,
                    monitorJumpLoading = false,
                    monitorHighlightedParcelId = null,
                    lastCode = if (jobChanged) null else it.lastCode,
                    lastParcelCount = if (jobChanged) null else it.lastParcelCount,
                    lastBoxCount = if (jobChanged) null else it.lastBoxCount,
                    lastScanSource = if (jobChanged) null else it.lastScanSource,
                    lastItemNumbers = if (jobChanged) emptyList() else it.lastItemNumbers,
                    lastExtData = if (jobChanged) null else it.lastExtData,
                    lastScanTime = if (jobChanged) null else it.lastScanTime,
                    error = null, 
                    isScanning = false
                ) 
            }
            if (job != null && ::scanJobMonitorRepo.isInitialized) {
                startScanJobMonitor(job.id)
            } else if (job == null) {
                refreshScanJobsKeepingMessage()
            }
        }
    }

    fun openMonitorRegister() {
        monitorDetailJob?.cancel()
        _state.update {
            it.copy(
                monitorSelectedScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES),
                monitorDetailSnapshot = null,
                monitorDetailLoading = false,
                monitorError = null,
                monitorHighlightedParcelId = null
            )
        }
    }

    fun openMonitorBox(box: ScanJobMonitorBox) {
        val scope = monitorScopeForBox(box) ?: return
        loadMonitorDetail(scope, highlightedParcelId = null)
    }

    fun toggleMonitorAutoFollow() {
        _state.update { it.copy(monitorAutoFollow = !it.monitorAutoFollow) }
    }

    fun setMonitorJumpNumber(value: String) {
        _state.update { it.copy(monitorJumpNumber = value, monitorError = null) }
    }

    fun jumpToMonitorNumber() {
        val jobId = state.value.selectedScanJob?.id ?: return
        val number = state.value.monitorJumpNumber.trim()
        if (number.isBlank()) {
            _state.update {
                it.copy(monitorError = getApplication<Application>().getString(R.string.monitor_jump_blank))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(monitorJumpLoading = true, monitorError = null) }
            try {
                val target = scanJobMonitorRepo.resolveTarget(jobId, number)
                when (target.kind) {
                    ScanJobMonitorTargetKinds.BOX -> {
                        val boxId = target.boxId
                        if (boxId == null) {
                            _state.update {
                                it.copy(monitorError = getApplication<Application>().getString(R.string.monitor_jump_not_found))
                            }
                        } else {
                            loadMonitorDetail(
                                ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = boxId),
                                highlightedParcelId = null
                            )
                        }
                    }
                    ScanJobMonitorTargetKinds.PARCEL -> {
                        val parcelId = target.parcelId
                        val scope = monitorScopeForTarget(target.area, target.boxId, target.bucketIndex)
                        if (parcelId == null || scope == null) {
                            _state.update {
                                it.copy(monitorError = getApplication<Application>().getString(R.string.monitor_jump_not_found))
                            }
                        } else {
                            loadMonitorDetail(scope, highlightedParcelId = parcelId)
                        }
                    }
                    else -> {
                        _state.update {
                            it.copy(
                                monitorError = getApplication<Application>().getString(R.string.monitor_jump_not_found),
                                monitorHighlightedParcelId = null
                            )
                        }
                    }
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Log.e(javaClass.simpleName, "Failed to resolve scan job monitor target", ex)
                _state.update {
                    it.copy(monitorError = getApplication<Application>().getString(R.string.monitor_jump_error))
                }
            } finally {
                _state.update { it.copy(monitorJumpLoading = false) }
            }
        }
    }

    private fun monitorScopeForTarget(area: Int?, boxId: Int?, bucketIndex: Int?): ScanJobMonitorScope? {
        return when (area) {
            ScanJobMonitorAreas.BOX -> boxId?.let { ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = it) }
            ScanJobMonitorAreas.UNASSIGNED -> ScanJobMonitorScope(
                ScanJobMonitorAreas.UNASSIGNED,
                bucketIndex = bucketIndex ?: 0
            )
            else -> null
        }
    }

    private fun startScanJobMonitor(scanJobId: Int) {
        monitorScopeVersion += 1
        val version = monitorScopeVersion
        monitorLatestScanCodeId = null
        monitorJob?.cancel()
        monitorDetailJob?.cancel()

        monitorJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    monitorLoading = true,
                    monitorDetailLoading = false,
                    monitorSnapshot = null,
                    monitorDetailSnapshot = null,
                    monitorSelectedScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES),
                    monitorError = null,
                    monitorJumpLoading = false,
                    monitorHighlightedParcelId = null
                )
            }

            try {
                scanJobMonitorRepo.stop()

                val registerScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES)
                val snapshot = scanJobMonitorRepo.loadSnapshot(scanJobId, registerScope)
                applyMonitorSnapshot(snapshot, version, followLatest = true)

                if (snapshot.status == SCAN_JOB_STATUS_IN_PROGRESS) {
                    scanJobMonitorRepo.observe(
                        scanJobId = scanJobId,
                        scope = registerScope,
                        onSnapshot = { nextSnapshot ->
                            viewModelScope.launch {
                                applyMonitorSnapshot(nextSnapshot, version, followLatest = true)
                            }
                        },
                        onClosed = { closedScanJobId, status ->
                            viewModelScope.launch {
                                handleMonitorClosed(closedScanJobId, status, version)
                            }
                        },
                        onConnectionClosed = { exception ->
                            viewModelScope.launch {
                                handleMonitorConnectionClosed(exception, version)
                            }
                        }
                    )

                    if (version == monitorScopeVersion) {
                        _state.update {
                            it.copy(monitorError = null)
                        }
                    }
                } else if (version == monitorScopeVersion) {
                    returnToScanJobSelectionForInactiveJob(snapshot.status)
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Log.e(javaClass.simpleName, "Failed to start scan job monitor", ex)
                if (version == monitorScopeVersion) {
                    _state.update {
                        it.copy(
                            monitorError = getApplication<Application>().getString(R.string.monitor_error_load)
                        )
                    }
                }
            } finally {
                if (version == monitorScopeVersion) {
                    _state.update { it.copy(monitorLoading = false) }
                }
            }
        }
    }

    private fun applyMonitorSnapshot(
        snapshot: ScanJobMonitorSnapshot,
        version: Int,
        followLatest: Boolean
    ) {
        if (version != monitorScopeVersion || snapshot.scanJobId != state.value.selectedScanJob?.id) {
            return
        }

        _state.update {
            it.copy(
                monitorSnapshot = snapshot,
                monitorError = null
            )
        }

        if (!followLatest || !state.value.monitorAutoFollow) {
            return
        }

        val latestScanId = snapshot.latestScan?.scanCodeId ?: return
        if (latestScanId == monitorLatestScanCodeId) {
            return
        }
        monitorLatestScanCodeId = latestScanId

        val nextScope = latestScanScope(snapshot) ?: return
        if (!sameMonitorScope(nextScope, state.value.monitorSelectedScope)) {
            if (nextScope.area == ScanJobMonitorAreas.BOXES) {
                openMonitorRegister()
            } else {
                loadMonitorDetail(nextScope, highlightedParcelId = null)
            }
        }
    }

    private fun loadMonitorDetail(scope: ScanJobMonitorScope, highlightedParcelId: Int? = null) {
        val jobId = state.value.selectedScanJob?.id ?: return
        val version = monitorScopeVersion

        if (scope.area == ScanJobMonitorAreas.BOXES) {
            openMonitorRegister()
            return
        }

        monitorDetailJob?.cancel()
        monitorDetailJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    monitorSelectedScope = scope,
                    monitorDetailLoading = true,
                    monitorError = null,
                    monitorHighlightedParcelId = highlightedParcelId
                )
            }

            try {
                val snapshot = scanJobMonitorRepo.loadSnapshot(jobId, scope)
                if (version == monitorScopeVersion && jobId == state.value.selectedScanJob?.id) {
                    _state.update {
                        it.copy(
                            monitorDetailSnapshot = snapshot,
                            monitorDetailLoading = false,
                            monitorError = null,
                            monitorHighlightedParcelId = highlightedParcelId
                        )
                    }
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Log.e(javaClass.simpleName, "Failed to load scan job monitor detail", ex)
                if (version == monitorScopeVersion) {
                    _state.update {
                        it.copy(
                            monitorDetailLoading = false,
                            monitorError = getApplication<Application>().getString(R.string.monitor_error_load)
                        )
                    }
                }
            }
        }
    }

    private suspend fun stopScanJobMonitor() {
        monitorScopeVersion += 1
        monitorLatestScanCodeId = null
        monitorJob?.cancel()
        monitorDetailJob?.cancel()
        if (::scanJobMonitorRepo.isInitialized) {
            scanJobMonitorRepo.stop()
        }
        _state.update {
            it.copy(
                monitorSnapshot = null,
                monitorDetailSnapshot = null,
                monitorSelectedScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES),
                monitorLoading = false,
                monitorDetailLoading = false,
                monitorError = null,
                monitorJumpLoading = false,
                monitorHighlightedParcelId = null
            )
        }
    }

    private suspend fun handleMonitorClosed(scanJobId: Int, status: Int, version: Int) {
        if (version != monitorScopeVersion || scanJobId != state.value.selectedScanJob?.id) {
            return
        }

        returnToScanJobSelectionForInactiveJob(status)
    }

    private suspend fun returnToScanJobSelectionForInactiveJob(status: Int) {
        scanJobMonitorRepo.stop()
        val message = getApplication<Application>().getString(
            R.string.monitor_closed,
            scanJobStatusText(getApplication(), status)
        )
        monitorScopeVersion += 1
        monitorLatestScanCodeId = null
        monitorDetailJob?.cancel()
        _state.update {
            it.copy(
                selectedScanJob = null,
                selectedScanJobTypeDisplay = null,
                monitorSnapshot = null,
                monitorDetailSnapshot = null,
                monitorSelectedScope = ScanJobMonitorScope(ScanJobMonitorAreas.BOXES),
                monitorLoading = false,
                monitorDetailLoading = false,
                monitorError = null,
                monitorJumpNumber = "",
                monitorJumpLoading = false,
                monitorHighlightedParcelId = null,
                isScanning = false,
                lastCode = null,
                lastParcelCount = null,
                lastBoxCount = null,
                lastScanSource = null,
                lastItemNumbers = emptyList(),
                lastExtData = null,
                lastScanTime = null,
                scanResultColor = ScanResultColor.NONE,
                error = message
            )
        }
        refreshScanJobsKeepingMessage()
    }

    private suspend fun refreshScanJobsKeepingMessage() {
        if (!::scanJobRepo.isInitialized) {
            return
        }
        val preservedMessage = _state.value.error
        loadScanJobsInternal(clearError = false)
        _state.update { it.copy(error = preservedMessage) }
    }

    fun dismissMessage() {
        _state.update { it.copy(error = null) }
    }

    private fun handleMonitorConnectionClosed(exception: Throwable?, version: Int) {
        if (version != monitorScopeVersion || exception == null) {
            return
        }

        Log.e(javaClass.simpleName, "Scan job monitor connection closed", exception)
        _state.update {
            it.copy(
                monitorError = getApplication<Application>().getString(R.string.monitor_error_connection)
            )
        }
    }

    fun startScanning() {
        _state.update { it.copy(isScanning = true, error = null) }
    }

    fun stopScanning() {
        _state.update { it.copy(isScanning = false) }
    }

    fun onScanned(code: String) {
        // Ignore scans when not in scanning mode
        if (!state.value.isScanning) {
            Log.d(javaClass.simpleName, "Scan received but not in scanning mode, ignoring: $code")
            return
        }
        
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
                        lastParcelCount = result.parcelCount,
                        lastBoxCount = result.boxCount,
                        lastScanSource = result.scanSource,
                        lastItemNumbers = result.itemNumbers,
                        lastExtData = result.extData,
                        lastScanTime = OffsetDateTime.now().toString(),
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
                    stopScanJobMonitor()
                    // Return to JobSelectionScreen by deselecting the job
                    _state.update { 
                        it.copy(
                            selectedScanJob = null,
                            selectedScanJobTypeDisplay = null,
                            isScanning = false,
                            lastCode = null,
                            lastParcelCount = null,
                            lastBoxCount = null,
                            lastScanSource = null,
                            lastItemNumbers = emptyList(),
                            lastExtData = null,
                            lastScanTime = null,
                            error = getApplication<Application>().getString(R.string.scan_error_job_invalid),
                            scanResultColor = ScanResultColor.NONE
                        )
                    }
                    refreshScanJobsKeepingMessage()
                } else {
                    _state.update { 
                        it.copy(
                            lastCode = code,
                            lastParcelCount = null,
                            lastBoxCount = null,
                            lastScanSource = null,
                            lastItemNumbers = emptyList(),
                            lastExtData = null,
                            lastScanTime = OffsetDateTime.now().toString(),
                            error = getApplication<Application>().getString(R.string.scan_error_server),
                            scanResultColor = ScanResultColor.SERVER_ERROR
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
            scanJobListMonitorVersion += 1
            scanJobListMonitorJob?.cancel()
            scanJobListRefreshJob?.cancel()
            stopScanJobMonitor()
            if (::scanJobMonitorRepo.isInitialized) {
                scanJobMonitorRepo.stopScanJobs()
            }
            loginRepo.logout()
            colorResetJob?.cancel()
            _state.update { 
                MainState(email = it.email, password = "") 
            }
        }
    }
}

private const val SCAN_JOB_LIST_REFRESH_DEBOUNCE_MS = 300L
