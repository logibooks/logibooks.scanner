// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.app.Application
import android.content.res.Configuration
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import consulting.sw.logiscanner.BuildConfig
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.StickerTemplates
import consulting.sw.logiscanner.net.SCAN_JOB_STATUS_IN_PROGRESS
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ScanJobMonitorTargetKinds
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.printer.BluetoothPrinterClient
import consulting.sw.logiscanner.printer.BluetoothPrinterDevice
import consulting.sw.logiscanner.printer.KgtStickerPrintResult
import consulting.sw.logiscanner.printer.KgtStickerPrintService
import consulting.sw.logiscanner.printer.ParcelSticker
import consulting.sw.logiscanner.printer.PrinterPermissionMissingException
import consulting.sw.logiscanner.printer.PrinterTestReport
import consulting.sw.logiscanner.printer.TscStickerRenderer
import consulting.sw.logiscanner.printer.TajikistanExportSticker
import consulting.sw.logiscanner.printer.validateForPrinting
import consulting.sw.logiscanner.repo.LoginRepository
import consulting.sw.logiscanner.repo.ScanJobMonitorRepository
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import consulting.sw.logiscanner.repo.ScanJobRepository
import consulting.sw.logiscanner.repo.ScanRepository
import consulting.sw.logiscanner.store.RelabelingSubmode
import consulting.sw.logiscanner.store.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
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

private val russianSpeechLocale = Locale.forLanguageTag("ru-RU")

enum class ScanResultColor {
    NONE, NOT_FOUND, OK, ISSUE, SERVER_ERROR, IGNORED
}

fun determineScanResultColor(result: ScanResultItem): ScanResultColor {
    return when {
        result.isLookupBoxScan() -> ScanResultColor.OK
        result.lookupOutcome in setOf("alreadyConfirmed", "noOutstanding") -> ScanResultColor.IGNORED
        result.lookupOutcome in setOf("inspectBox", "ambiguous") || result.hasIssues -> ScanResultColor.ISSUE
        result.count == 0 -> ScanResultColor.NOT_FOUND
        else -> ScanResultColor.OK
    }
}

internal data class MainState(
    val email: String = "",
    val password: String = "",
    val externalScannerEnabled: Boolean = false,
    val isLoggedIn: Boolean = false,
    val settingsOpen: Boolean = false,
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
    val bulkyItemsMode: Int = BulkyItemsModes.OFF,
    val printerAutoPrintEnabled: Boolean = false,
    val kgtVoiceEnabled: Boolean = false,
    val relabelingSubmode: RelabelingSubmode = RelabelingSubmode.KGT,
    val printerBluetoothAddress: String? = null,
    val bondedPrinters: List<BluetoothPrinterDevice> = emptyList(),
    val printerLoading: Boolean = false,
    val selectionPrinterLoading: Boolean = false,
    val selectionPrintFailures: Map<SelectionPrintKey, SelectionPrintFailure> = emptyMap(),
    @get:StringRes val printerMessage: Int? = null,
    val printerError: String? = null,
    @get:StringRes val printerTestError: Int? = null,
    val printerTestRunning: Boolean = false,
    val printerTestReport: PrinterTestReport? = null,
    val voiceTestFailed: Boolean = false,
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
    val lastLookupBox: LookupBoxResult? = null,
    val lastSelectedParcels: List<SelectedParcelScan> = emptyList(),
    val parcelSelection: ParcelSelectionState? = null,
    val lastExtId: String? = null,
    val lastTajikistanExportSticker: TajikistanExportSticker? = null,
    val lastScanTime: String? = null,
    val scanResultColor: ScanResultColor = ScanResultColor.NONE,
    val error: String? = null
)

internal fun openSettingsState(state: MainState): MainState =
    if (state.parcelSelection == null) state.copy(settingsOpen = true) else state

internal fun openParcelSelectionState(state: MainState, selection: ParcelSelectionState): MainState =
    state.copy(
        parcelSelection = selection,
        lastCode = null,
        lastParcelCount = null,
        lastBoxCount = null,
        lastScanSource = null,
        lastItemNumbers = emptyList(),
        lastExtData = null,
        lastLookupBox = null,
        lastSelectedParcels = emptyList(),
        lastExtId = null,
        lastTajikistanExportSticker = null,
        lastScanTime = null
    )

internal fun closeSettingsState(state: MainState): MainState =
    state.copy(
        settingsOpen = false, printerTestError = null, printerTestRunning = false,
        printerTestReport = null, voiceTestFailed = false
    )

internal fun configuredPrinterAddress(state: MainState): String? =
    state.printerBluetoothAddress?.takeIf { state.isLoggedIn && it.isNotBlank() }

internal fun stoppedScanWarningState(state: MainState, message: String): MainState {
    return state.copy(
        error = message,
        scanResultColor = ScanResultColor.IGNORED
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val bluetoothPrinter = BluetoothPrinterClient(application, viewModelScope)
    private val stickerPrintService = KgtStickerPrintService(
        renderer = TscStickerRenderer(),
        client = bluetoothPrinter
    )
    private val _state = MutableStateFlow(MainState())
    internal val state: StateFlow<MainState> = _state.asStateFlow()

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
    private var scanSessionVersion = 0
    private val scanInput = ScanInputQueue<AcceptedScan>()
    private var selectionOptions: ScanProcessingOptions? = null
    private var selectionPrintJob: Job? = null
    private var selectionPrintAccountVersion = 0
    private var signingOut = false
    
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var voiceTestUtteranceIds = emptySet<String>()
    private var printerTestJob: Job? = null
    private class PrinterTestAttempt(val address: String)
    private var printerTestAttempt: PrinterTestAttempt? = null
    private var pendingPrinterTestPermission: PrinterTestAttempt? = null

    private class MonitorStickerPrint(
        val jobId: Int,
        val target: MonitorStickerTarget,
        val address: String,
        val repository: ScanJobMonitorRepository
    ) {
        var worker: Job? = null
    }

    private var monitorStickerPrint: MonitorStickerPrint? = null

    init {
        viewModelScope.launch {
            state.collect { updatePrinterSession() }
        }

        viewModelScope.launch {
            settingsStore.externalScannerEnabled().collect { enabled ->
                _state.update { it.copy(externalScannerEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            settingsStore.printerAutoPrintEnabled().collect { enabled ->
                _state.update { it.copy(printerAutoPrintEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            settingsStore.kgtVoiceEnabled().collect { enabled ->
                _state.update {
                    it.copy(
                        kgtVoiceEnabled = enabled,
                        bulkyItemsMode = applyRelabelingVoiceSetting(
                            it.relabelingSubmode,
                            it.bulkyItemsMode,
                            enabled
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsStore.relabelingSubmode().collect { submode ->
                _state.update {
                    val printerSelected = hasSelectedPrinter(it.printerBluetoothAddress)
                    it.copy(
                        relabelingSubmode = submode,
                        bulkyItemsMode = normalizeRelabelingMode(
                            it.selectedScanJob,
                            submode,
                            it.bulkyItemsMode,
                            it.kgtVoiceEnabled,
                            printerSelected
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsStore.printerBluetoothAddress().collect { address ->
                if (address != state.value.printerBluetoothAddress) {
                    cancelPrinterTest()
                    cancelMonitorStickerPrint()
                }
                _state.update {
                    val nextSubmode = if (address.isNullOrBlank()) RelabelingSubmode.KGT else it.relabelingSubmode
                    it.copy(
                        printerBluetoothAddress = address,
                        relabelingSubmode = nextSubmode,
                        bulkyItemsMode = normalizeRelabelingMode(
                            it.selectedScanJob,
                            nextSubmode,
                            it.bulkyItemsMode,
                            it.kgtVoiceEnabled,
                            hasSelectedPrinter(address)
                        )
                    )
                }
            }
        }

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
                val result = tts?.setLanguage(russianSpeechLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(javaClass.simpleName, "Russian language not supported for TTS")
                    ttsReady = false
                } else {
                    // Try to select a male voice
                    tts?.voices?.find { voice ->
                        voice.locale == russianSpeechLocale && voice.name.lowercase().contains("male")
                    }?.let { maleVoice ->
                        tts?.voice = maleVoice
                    }
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) = Unit
                        override fun onDone(utteranceId: String) = Unit

                        @Deprecated("Older speech engines call this overload")
                        override fun onError(utteranceId: String) {
                            onError(utteranceId, TextToSpeech.ERROR)
                        }

                        override fun onError(utteranceId: String, errorCode: Int) {
                            viewModelScope.launch { showVoiceTestFailure(utteranceId, errorCode) }
                        }
                    })
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
        bluetoothPrinter.selectPrinter(null)
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
    fun openSettings() = _state.update(::openSettingsState)
    fun closeSettings() {
        voiceTestUtteranceIds = emptySet()
        cancelPrinterTest()
        _state.update(::closeSettingsState)
    }

    fun testVoice() {
        if (!state.value.settingsOpen) return
        val testId = "voice_test_${System.nanoTime()}"
        val titleId = "${testId}_title"
        val phraseId = "${testId}_phrase"
        voiceTestUtteranceIds = setOf(titleId, phraseId)
        _state.update { it.copy(voiceTestFailed = false) }

        val speech = tts
        if (!ttsReady || speech == null) {
            showVoiceTestFailure(titleId, TextToSpeech.ERROR)
            return
        }

        val russianVoice = speech.voice
        val englishStatus = speech.setLanguage(Locale.ENGLISH)
        if (englishStatus < TextToSpeech.LANG_AVAILABLE) {
            showVoiceTestFailure(titleId, englishStatus)
            return
        }

        val app = getApplication<Application>()
        val title = app.getString(R.string.settings_test_title, BuildConfig.VERSION_NAME)
        val titleStatus = try {
            // Android captures the selected voice when each utterance is queued.
            speech.speak(title, TextToSpeech.QUEUE_FLUSH, null, titleId)
        } finally {
            ttsReady = russianVoice?.let(speech::setVoice) == TextToSpeech.SUCCESS ||
                speech.setLanguage(russianSpeechLocale) >= TextToSpeech.LANG_AVAILABLE
        }
        if (titleStatus != TextToSpeech.SUCCESS || !ttsReady) {
            showVoiceTestFailure(titleId, if (ttsReady) titleStatus else TextToSpeech.ERROR)
            return
        }

        val phrase = app.getString(R.string.settings_test_voice_phrase)
        val phraseStatus = speech.speak(phrase, TextToSpeech.QUEUE_ADD, null, phraseId)
        if (phraseStatus != TextToSpeech.SUCCESS) {
            showVoiceTestFailure(phraseId, phraseStatus)
        }
    }

    private fun showVoiceTestFailure(utteranceId: String, errorCode: Int) {
        if (utteranceId !in voiceTestUtteranceIds) return
        voiceTestUtteranceIds = emptySet()
        Log.w(javaClass.simpleName, "Voice test failed with TTS status $errorCode")
        _state.update { it.copy(voiceTestFailed = true) }
    }

    fun reconnectPrinterIfNeeded() {
        updatePrinterSession()
        if (!signingOut) configuredPrinterAddress(state.value)?.let(bluetoothPrinter::warmUp)
    }

    private fun updatePrinterSession() {
        bluetoothPrinter.selectPrinter(if (signingOut) null else configuredPrinterAddress(state.value))
    }

    fun setExternalScannerEnabled(value: Boolean) {
        _state.update { it.copy(externalScannerEnabled = value) }
        viewModelScope.launch {
            settingsStore.setExternalScannerEnabled(value)
        }
    }

    fun setPrinterAutoPrintEnabled(value: Boolean) {
        if (value && !hasSelectedPrinter(state.value.printerBluetoothAddress)) {
            return
        }
        // Let the preference collector apply each toggle so an older save cannot undo a newer UI update.
        _state.update { it.copy(printerError = null, printerMessage = null) }
        viewModelScope.launch {
            settingsStore.setPrinterAutoPrintEnabled(value)
        }
    }

    fun setKgtVoiceEnabled(value: Boolean) {
        _state.update {
            it.copy(
                kgtVoiceEnabled = value,
                bulkyItemsMode = applyRelabelingVoiceSetting(
                    it.relabelingSubmode,
                    it.bulkyItemsMode,
                    value
                )
            )
        }
        viewModelScope.launch {
            settingsStore.setKgtVoiceEnabled(value)
        }
    }

    fun setRelabelingSubmode(value: RelabelingSubmode) {
        if (value == RelabelingSubmode.FULL && !hasSelectedPrinter(state.value.printerBluetoothAddress)) {
            return
        }

        _state.update {
            val printerSelected = hasSelectedPrinter(it.printerBluetoothAddress)
            it.copy(
                relabelingSubmode = value,
                bulkyItemsMode = normalizeRelabelingMode(
                    it.selectedScanJob,
                    value,
                    it.bulkyItemsMode,
                    it.kgtVoiceEnabled,
                    printerSelected
                ),
                printerError = null,
                printerMessage = null
            )
        }
        viewModelScope.launch {
            settingsStore.setRelabelingSubmode(value)
        }
    }

    fun setPrinterBluetoothAddress(value: String?) {
        if (value != state.value.printerBluetoothAddress) {
            cancelPrinterTest()
            cancelMonitorStickerPrint()
        }
        // Apply the address through the preference collector so an older save cannot reconnect
        // the previous printer after a newer selection has already been shown.
        _state.update {
            it.copy(
                printerError = null,
                printerMessage = null,
                printerTestError = null
            )
        }
        viewModelScope.launch {
            if (value.isNullOrBlank()) {
                settingsStore.setRelabelingSubmode(RelabelingSubmode.KGT)
            }
            settingsStore.setPrinterBluetoothAddress(value)
        }
    }

    fun refreshPrinters() {
        viewModelScope.launch {
            _state.update {
                it.copy(printerLoading = true, printerError = null, printerMessage = null, printerTestError = null)
            }
            try {
                val printers = stickerPrintService.listBondedPrinters()
                val missingPrinterMessage = selectedPrinterMissingMessage(
                    printers,
                    state.value.printerBluetoothAddress
                )
                _state.update {
                    it.copy(
                        bondedPrinters = printers,
                        printerError = missingPrinterMessage,
                        printerMessage = R.string.printer_no_paired.takeIf { printers.isEmpty() }
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Log.e(javaClass.simpleName, "Failed to refresh printers", ex)
                _state.update {
                    it.copy(
                        printerError = if (ex is PrinterPermissionMissingException) {
                            getApplication<Application>().getString(R.string.printer_permission_missing)
                        } else {
                            getApplication<Application>().getString(R.string.printer_list_failed)
                        },
                        printerMessage = null
                    )
                }
            } finally {
                _state.update { it.copy(printerLoading = false) }
            }
        }
    }

    fun setPrinterPermissionDenied() {
        _state.update {
            it.copy(
                printerError = getApplication<Application>().getString(R.string.printer_permission_missing),
                printerMessage = null,
                printerTestError = null
            )
        }
    }

    /** Returns true when the activity needs to launch the Android permission prompt. */
    internal fun requestPrinterTest(hasPermission: Boolean): Boolean {
        val current = state.value
        val address = current.printerBluetoothAddress
        if (!current.settingsOpen || signingOut || current.printerLoading || current.printerTestRunning ||
            address.isNullOrBlank() || (!hasPermission && pendingPrinterTestPermission != null)
        ) return false

        val attempt = PrinterTestAttempt(address)
        printerTestAttempt = attempt
        _state.update {
            it.copy(printerTestRunning = true, printerTestError = null, printerTestReport = null)
        }
        if (hasPermission) startPrinterTest(attempt) else pendingPrinterTestPermission = attempt
        return !hasPermission
    }

    internal fun finishPrinterTestPermission(granted: Boolean) {
        val attempt = pendingPrinterTestPermission ?: return
        pendingPrinterTestPermission = null
        if (printerTestAttempt !== attempt) return
        if (granted) {
            startPrinterTest(attempt)
        } else {
            printerTestAttempt = null
            _state.update {
                it.copy(printerTestRunning = false, printerTestError = R.string.printer_permission_missing)
            }
        }
    }

    private fun startPrinterTest(attempt: PrinterTestAttempt) {
        val title = getApplication<Application>().getString(R.string.settings_test_title, BuildConfig.VERSION_NAME)
        updatePrinterSession()
        printerTestJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val report = stickerPrintService.printTestPage(attempt.address, title)
                if (printerTestAttempt === attempt) {
                    _state.update { it.copy(printerTestReport = report) }
                }
            } finally {
                if (printerTestAttempt === attempt) {
                    printerTestAttempt = null
                    _state.update { it.copy(printerTestRunning = false) }
                }
            }
        }
    }

    private fun cancelPrinterTest() {
        printerTestAttempt = null
        printerTestJob?.cancel()
        printerTestJob = null
        _state.update { it.copy(printerTestRunning = false, printerTestReport = null, printerTestError = null) }
    }

    fun printKgtSticker(code: String) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            printKgtStickerInternal(code)
        }
    }

    fun printParcelSticker(sticker: ParcelSticker) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            printParcelStickerInternal(sticker)
        }
    }

    internal fun prepareMonitorStickerPrint(jobId: Int, target: MonitorStickerTarget): ((Boolean) -> Unit)? {
        val current = state.value
        val address = configuredPrinterAddress(current) ?: return null
        if (signingOut || current.printerLoading || current.selectionPrinterLoading || current.selectedScanJob?.id != jobId
            || !::scanJobMonitorRepo.isInitialized) return null

        val command = MonitorStickerPrint(jobId, target, address, scanJobMonitorRepo)
        monitorStickerPrint = command
        _state.update { it.copy(printerLoading = true, printerError = null, printerMessage = null) }
        return { granted -> finishMonitorStickerPermission(command, granted) }
    }

    private fun finishMonitorStickerPermission(command: MonitorStickerPrint, granted: Boolean) {
        if (monitorStickerPrint !== command || command.worker != null) return
        if (!granted) {
            cancelMonitorStickerPrint()
            setPrinterPermissionDenied()
            return
        }

        command.worker = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val target = command.target
                val result = stickerPrintService.printServerSticker(command.address, target.template) {
                    when (target.template) {
                        StickerTemplates.WBR_BOX, StickerTemplates.OZON_BOX ->
                            command.repository.loadBoxLabel(target.id, target.template)
                        StickerTemplates.OZON_PARCEL ->
                            command.repository.loadParcelLabel(command.jobId, target.id, target.template)
                        else -> error("Unsupported monitor label template")
                    }
                }
                if (monitorStickerPrint === command) applyPrinterResult(result)
            } finally {
                if (monitorStickerPrint === command) {
                    monitorStickerPrint = null
                    _state.update { it.copy(printerLoading = false) }
                }
            }
        }
        command.worker?.start()
    }

    internal fun cancelPendingMonitorStickerPermission() {
        if (monitorStickerPrint?.worker == null) cancelMonitorStickerPrint()
    }

    private fun cancelMonitorStickerPrint() {
        val command = monitorStickerPrint ?: return
        monitorStickerPrint = null
        command.worker?.cancel()
        _state.update { it.copy(printerLoading = false) }
    }

    suspend fun loadTajikistanInvoice(scanJobId: Int, parcelId: Int): ByteArray {
        return scanJobMonitorRepo.loadTajikistanInvoice(scanJobId, parcelId)
    }

    fun printTajikistanInvoice(scanJobId: Int, payload: ByteArray) {
        val current = state.value
        val job = current.selectedScanJob ?: return
        val address = current.printerBluetoothAddress
        if (job.id != scanJobId || job.stickerTemplate != StickerTemplates.TAJIKISTAN_EXPORT ||
            address.isNullOrBlank() || current.printerLoading
        ) {
            return
        }
        _state.update { it.copy(printerLoading = true, printerError = null, printerMessage = null) }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                applyPrinterResult(stickerPrintService.printTajikistanInvoice(address, payload))
            } finally {
                _state.update { it.copy(printerLoading = false) }
            }
        }
    }

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
                        if (!signingOut && loginRepo.token == token) logout()
                    }
                }
                scanJobRepo = ScanJobRepository(url, token, unauthorizedHandler)
                cancelMonitorStickerPrint()
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
            if (signingOut || state.value.parcelSelection != null) return@launch
            val jobChanged = state.value.selectedScanJob?.id != job?.id
            if (jobChanged && job != null) {
                resetScanSession()
                cancelMonitorStickerPrint()
            }
            val nextBulkyItemsMode = normalizeRelabelingMode(
                job,
                state.value.relabelingSubmode,
                state.value.bulkyItemsMode,
                state.value.kgtVoiceEnabled,
                hasSelectedPrinter(state.value.printerBluetoothAddress)
            )
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
                    bulkyItemsMode = nextBulkyItemsMode,
                    monitorJumpNumber = if (jobChanged) "" else it.monitorJumpNumber,
                    monitorJumpLoading = false,
                    monitorHighlightedParcelId = null,
                    lastCode = if (jobChanged) null else it.lastCode,
                    lastParcelCount = if (jobChanged) null else it.lastParcelCount,
                    lastBoxCount = if (jobChanged) null else it.lastBoxCount,
                    lastScanSource = if (jobChanged) null else it.lastScanSource,
                    lastItemNumbers = if (jobChanged) emptyList() else it.lastItemNumbers,
                    lastExtData = if (jobChanged) null else it.lastExtData,
                    lastLookupBox = if (jobChanged) null else it.lastLookupBox,
                    lastSelectedParcels = if (jobChanged) emptyList() else it.lastSelectedParcels,
                    lastExtId = if (jobChanged) null else it.lastExtId,
                    lastTajikistanExportSticker = if (jobChanged) null else it.lastTajikistanExportSticker,
                    lastScanTime = if (jobChanged) null else it.lastScanTime,
                    error = null, 
                    isScanning = false
                ) 
            }
            if (job != null && ::scanJobMonitorRepo.isInitialized) {
                startScanJobMonitor(job.id)
            } else if (job == null) {
                stopScanJobMonitor()
                if (state.value.selectedScanJob == null) refreshScanJobsKeepingMessage()
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

    fun toggleBulkyItemsMode() {
        _state.update {
            it.copy(
                bulkyItemsMode = nextRelabelingMode(
                    job = it.selectedScanJob,
                    submode = it.relabelingSubmode,
                    currentMode = it.bulkyItemsMode,
                    voiceEnabled = it.kgtVoiceEnabled,
                    printerSelected = hasSelectedPrinter(it.printerBluetoothAddress)
                )
            )
        }
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
                applyMonitorSnapshot(snapshot, version)

                if (snapshot.status == SCAN_JOB_STATUS_IN_PROGRESS) {
                    scanJobMonitorRepo.observe(
                        scanJobId = scanJobId,
                        scope = registerScope,
                        onSnapshot = { nextSnapshot ->
                            viewModelScope.launch {
                                applyMonitorSnapshot(nextSnapshot, version)
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
        version: Int
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
        resetScanSession()
        cancelMonitorStickerPrint()
        monitorScopeVersion += 1
        monitorJob?.cancel()
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
                monitorJumpLoading = false,
                monitorHighlightedParcelId = null
            )
        }
        if (::scanJobMonitorRepo.isInitialized) {
            scanJobMonitorRepo.stop()
        }
    }

    private suspend fun handleMonitorClosed(scanJobId: Int, status: Int, version: Int) {
        if (version != monitorScopeVersion || scanJobId != state.value.selectedScanJob?.id) {
            return
        }

        returnToScanJobSelectionForInactiveJob(status)
    }

    private suspend fun returnToScanJobSelectionForInactiveJob(status: Int) {
        resetScanSession()
        cancelMonitorStickerPrint()
        val message = getApplication<Application>().getString(
            R.string.monitor_closed,
            scanJobStatusText(getApplication(), status)
        )
        monitorScopeVersion += 1
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
                bulkyItemsMode = BulkyItemsModes.OFF,
                isScanning = false,
                lastCode = null,
                lastParcelCount = null,
                lastBoxCount = null,
                lastScanSource = null,
                lastItemNumbers = emptyList(),
                lastExtData = null,
                lastLookupBox = null,
                lastSelectedParcels = emptyList(),
                lastExtId = null,
                lastScanTime = null,
                scanResultColor = ScanResultColor.NONE,
                error = message
            )
        }
        scanJobMonitorRepo.stop()
        if (state.value.selectedScanJob == null) refreshScanJobsKeepingMessage()
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
        _state.update { if (it.selectedScanJob == null) it else it.copy(isScanning = true, error = null) }
    }

    fun stopScanning() {
        _state.update { it.copy(isScanning = false) }
    }

    private fun resetScanSession() {
        // Queued and in-flight results belong to the job selection that accepted the scan.
        scanSessionVersion += 1
        scanInput.reset()
        selectionOptions = null
        tts?.stop()
        colorResetJob?.cancel()
        _state.update {
            it.copy(isScanning = false, isBusy = false, scanResultColor = ScanResultColor.NONE, parcelSelection = null)
        }
    }

    fun onScanned(code: String) {
        if (state.value.parcelSelection != null) {
            remindParcelSelection()
            return
        }
        val job = state.value.selectedScanJob ?: return
        // Ignore scans when not in scanning mode
        if (!state.value.isScanning) {
            Log.d(javaClass.simpleName, "Scan received but not in scanning mode, ignoring: $code")
            colorResetJob?.cancel()
            _state.update {
                stoppedScanWarningState(
                    it,
                    getApplication<Application>().getString(R.string.scan_stopped_warning)
                )
            }
            if (ttsReady) {
                tts?.speak(
                    getApplication<Application>().getString(R.string.scan_stopped_tts),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "scan_stopped_${System.currentTimeMillis()}"
                )
            }
            colorResetJob = viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                _state.update { it.copy(scanResultColor = ScanResultColor.NONE) }
            }
            return
        }
        val accepted = AcceptedScan(job, code, scanProcessingOptions(job))
        if (scanInput.begin(accepted)) processAcceptedScan(accepted)
    }

    private fun processAcceptedScan(accepted: AcceptedScan) {
        val (job, code, options) = accepted
        val sessionVersion = scanSessionVersion
        reconnectPrinterIfNeeded()
        
        // Cancel any pending color reset
        colorResetJob?.cancel()
        
        viewModelScope.launch {
            if (sessionVersion != scanSessionVersion) return@launch
            _state.update {
                it.copy(
                    isBusy = true,
                    error = null,
                    printerError = null,
                    printerMessage = null,
                    scanResultColor = ScanResultColor.NONE
                )
            }
            try {
                val result = scanRepo.scan(job.id, code, options.bulkyItemsMode)
                if (sessionVersion != scanSessionVersion) return@launch
                val selection = result.parcelSelection
                if (selection != null) {
                    selectionOptions = options
                    val pending = ParcelSelectionState(code, options.bulkyItemsMode, selection)
                    _state.update { openParcelSelectionState(it, pending) }
                    val discardedCodes = scanInput.discardQueued().map(AcceptedScan::code).distinct()
                    _state.update { it.copy(parcelSelection = pending.copy(discardedCodes = discardedCodes)) }
                    speakScanText(parcelSelectionSpeech(result.extData, parcelSelectionPrompt()))
                } else {
                    applyScanResult(job, code, result, options)
                }
            } catch (ex: Exception) {
                if (sessionVersion != scanSessionVersion) return@launch
                Log.e(javaClass.simpleName, "Scan failed", ex)
                
                // Check if it's a 400 Bad Request - job selection is no longer valid
                if (ex is HttpException && ex.code() == 400) {
                    // Return to JobSelectionScreen by deselecting the job
                    _state.update { 
                        it.copy(
                            selectedScanJob = null,
                            selectedScanJobTypeDisplay = null,
                            bulkyItemsMode = BulkyItemsModes.OFF,
                            isScanning = false,
                            lastCode = null,
                            lastParcelCount = null,
                            lastBoxCount = null,
                            lastScanSource = null,
                            lastItemNumbers = emptyList(),
                            lastExtData = null,
                            lastLookupBox = null,
                            lastSelectedParcels = emptyList(),
                            lastExtId = null,
                            lastTajikistanExportSticker = null,
                            lastScanTime = null,
                            error = getApplication<Application>().getString(R.string.scan_error_job_invalid),
                            scanResultColor = ScanResultColor.NONE
                        )
                    }
                    stopScanJobMonitor()
                    if (state.value.selectedScanJob == null) refreshScanJobsKeepingMessage()
                } else {
                    _state.update { 
                        it.copy(
                            lastCode = code,
                            lastParcelCount = null,
                            lastBoxCount = null,
                            lastScanSource = null,
                            lastItemNumbers = emptyList(),
                            lastExtData = null,
                            lastLookupBox = null,
                            lastSelectedParcels = emptyList(),
                            lastExtId = null,
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
                if (sessionVersion == scanSessionVersion) {
                    _state.update { it.copy(isBusy = false) }
                    val current = state.value
                    if (current.selectedScanJob?.id == job.id && current.parcelSelection == null) {
                        scanInput.finish()?.let(::processAcceptedScan)
                    } else {
                        scanInput.reset()
                    }
                }
            }
        }
    }

    private data class AcceptedScan(val job: ScanJob, val code: String, val options: ScanProcessingOptions)

    private data class ScanProcessingOptions(
        val printerSelected: Boolean,
        val submode: RelabelingSubmode,
        val relabelingMode: Int,
        val voiceEnabled: Boolean,
        val bulkyItemsMode: Int
    )

    private fun scanProcessingOptions(job: ScanJob): ScanProcessingOptions {
        val current = state.value
        val printerSelected = hasSelectedPrinter(current.printerBluetoothAddress)
        val submode = current.relabelingSubmode
        val voiceEnabled = submode == RelabelingSubmode.KGT && current.kgtVoiceEnabled
        val relabelingMode = normalizeRelabelingMode(job, submode, current.bulkyItemsMode, voiceEnabled, printerSelected)
        return ScanProcessingOptions(
            printerSelected, submode, relabelingMode, voiceEnabled,
            backendBulkyItemsMode(job, submode, relabelingMode, voiceEnabled)
        )
    }

    private fun applyScanResult(
        job: ScanJob,
        code: String,
        result: ScanResultItem,
        options: ScanProcessingOptions,
        selectionResult: Boolean = false
    ): SelectionPrintWork? {
        val (printerSelected, submode, relabelingMode, voiceEnabled, bulkyItemsMode) = options
        val autoPrintEnabled = state.value.printerAutoPrintEnabled
        val tajikistanValidation = if (
            result.stickerTemplate == StickerTemplates.TAJIKISTAN_EXPORT
        ) {
            result.exportSticker.validateForPrinting()
        } else {
            null
        }
        val tajikistanSticker = tajikistanValidation?.sticker
        val tajikistanAutoPrintAction = tajikistanExportStickerAutoPrintAction(
            submode,
            relabelingMode,
            printerSelected,
            job,
            result,
            hasPrintableSticker = tajikistanSticker != null
        )
        val stickerDataError = if (tajikistanAutoPrintAction == TajikistanExportStickerAutoPrintAction.MISSING_DATA) {
            tajikistanStickerDataError(getApplication<Application>(), requireNotNull(tajikistanValidation).issues)
        } else null
        _state.update {
            it.copy(
                lastCode = code,
                lastParcelCount = result.parcelCount,
                lastBoxCount = result.boxCount,
                lastScanSource = result.scanSource,
                lastItemNumbers = result.itemNumbers,
                lastExtData = result.extData,
                lastLookupBox = result.lookupBoxResult(),
                lastSelectedParcels = emptyList(),
                lastExtId = result.extId,
                lastTajikistanExportSticker = if (stickerDataError != null) {
                    null
                } else {
                    tajikistanSticker ?: it.lastTajikistanExportSticker
                },
                lastScanTime = result.scanTime?.takeIf { scanTime -> scanTime.isNotBlank() }
                    ?: OffsetDateTime.now().toString(),
                scanResultColor = determineScanResultColor(result),
                printerError = if (selectionResult) it.printerError else stickerDataError ?: it.printerError,
                printerMessage = if (!selectionResult && stickerDataError != null) null else it.printerMessage
            )
        }

        followLocalScanResult(result)

        val kgt = kgtStickerCode(result.extId).takeIf {
            shouldAutoPrintKgtSticker(autoPrintEnabled, job, bulkyItemsMode, result, printerSelected)
        }
        val export = tajikistanSticker.takeIf {
            tajikistanAutoPrintAction == TajikistanExportStickerAutoPrintAction.PRINT
        }
        if (!selectionResult) {
            kgt?.let(::printKgtSticker)
            export?.let { sticker ->
                viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    printTajikistanExportStickerInternal(sticker)
                }
            }
        }

        val extIdSpeechText = if (!result.extId.isNullOrBlank()) {
            getApplication<Application>().getString(R.string.bulky_items_number_speech, result.extId)
        } else {
            null
        }
        val speechText = scanResultSpeechText(
            submode,
            relabelingMode,
            voiceEnabled,
            extIdSpeechText,
            result.extData.takeUnless { selectionResult }
        )
        speakScanText(speechText)
        resetScanColor()
        return when {
            !selectionResult -> null
            stickerDataError != null -> SelectionPrintWork.Invalid(stickerDataError)
            kgt != null -> SelectionPrintWork.Kgt(kgt)
            export != null -> SelectionPrintWork.Export(export)
            else -> null
        }
    }

    private fun resetScanColor() {
        colorResetJob?.cancel()
        colorResetJob = viewModelScope.launch {
            delay(1500)
            _state.update { it.copy(scanResultColor = ScanResultColor.NONE) }
        }
    }

    private fun speakScanText(text: String) {
        if (text.isNotEmpty() && ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "scan_result_${System.nanoTime()}")
        }
    }

    private fun parcelSelectionPrompt(): String {
        val app = getApplication<Application>()
        val speechConfig = Configuration(app.resources.configuration).apply { setLocale(russianSpeechLocale) }
        return app.createConfigurationContext(speechConfig).getString(R.string.parcel_selection_prompt)
    }

    internal fun remindParcelSelection() {
        if (state.value.parcelSelection?.canEdit == true) {
            speakScanText(parcelSelectionPrompt())
        }
    }

    internal fun toggleParcelChoice(id: Int) {
        _state.update { current ->
            current.copy(parcelSelection = current.parcelSelection?.toggle(id))
        }
    }

    internal fun confirmParcelSelection() {
        val selected = state.value.parcelSelection?.checkedIds.orEmpty()
        if (selected.isNotEmpty()) submitParcelSelection(selected.sorted())
    }

    internal fun skipParcelSelection() = submitParcelSelection(emptyList())

    internal fun closeStaleParcelSelection() {
        state.value.parcelSelection?.takeIf { it.stale }?.let(::finishParcelSelection)
    }

    private fun finishParcelSelection(pending: ParcelSelectionState) {
        selectionOptions = null
        val warning = pending.discardedCodes.takeIf { it.isNotEmpty() }?.let { codes ->
            getApplication<Application>().getString(R.string.parcel_selection_rescan_codes, codes.joinToString(", "))
        }
        _state.update { it.copy(parcelSelection = null, error = warning, isBusy = false) }
    }

    private fun submitParcelSelection(ids: List<Int>) {
        val pending = state.value.parcelSelection ?: return
        val job = state.value.selectedScanJob ?: return
        if (!pending.canEdit) return
        val version = scanSessionVersion
        _state.update { it.copy(parcelSelection = pending.copy(submitting = true), isBusy = true) }
        viewModelScope.launch {
            val result = try {
                scanRepo.scan(job.id, pending.code, pending.bulkyItemsMode, pending.selection.id, ids)
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                if (version != scanSessionVersion) return@launch
                Log.e(javaClass.simpleName, "Could not submit parcel selection", ex)
                // The response can be lost after Core applies the scans. Recover through a fresh scan.
                _state.update { it.copy(parcelSelection = pending.copy(stale = true)) }
                return@launch
            }
            if (version != scanSessionVersion) return@launch
            val items = result.selectionResults
            if (items == null || items.size != ids.size) {
                _state.update { it.copy(parcelSelection = pending.copy(stale = true)) }
                return@launch
            }
            val options = selectionOptions ?: scanProcessingOptions(job).copy(bulkyItemsMode = pending.bulkyItemsMode)
            val choices = pending.selection.candidates.associateBy { it.parcelId }
            val printWork = mutableListOf<Pair<Int, SelectionPrintWork>>()
            val selected = ids.zip(items).map { (id, result) ->
                applyScanResult(job, pending.code, result, options, selectionResult = true)
                    ?.let { printWork.add(id to it) }
                SelectedParcelScan(id, choices[id]?.productName, result.lookupOutcome, result.extId,
                    result.exportSticker?.orderNumber, result.extData, result.hasIssues)
            }
            if (items.isNotEmpty()) {
                val colors = items.map(::determineScanResultColor)
                _state.update {
                    it.copy(
                        lastSelectedParcels = selected,
                        lastParcelCount = items.sumOf(ScanResultItem::parcelCount),
                        lastItemNumbers = items.flatMap(ScanResultItem::itemNumbers).distinct(),
                        scanResultColor = when {
                            ScanResultColor.ISSUE in colors -> ScanResultColor.ISSUE
                            ScanResultColor.OK in colors -> ScanResultColor.OK
                            else -> colors.last()
                        }
                    )
                }
            }
            finishParcelSelection(pending)
            enqueueSelectionPrinting(job.id, job.name, printWork)
        }
    }

    internal fun retrySelectionPrint(key: SelectionPrintKey) {
        val current = state.value
        if (signingOut || current.selectionPrinterLoading || current.printerLoading || configuredPrinterAddress(current) == null) return
        val failure = current.selectionPrintFailures[key] ?: return
        if (failure.work is SelectionPrintWork.Invalid) return
        enqueueSelectionPrinting(key.jobId, failure.jobName, listOf(key.parcelId to failure.work))
    }

    private fun enqueueSelectionPrinting(jobId: Int, jobName: String, work: List<Pair<Int, SelectionPrintWork>>) {
        if (work.isEmpty()) return
        val prior = selectionPrintJob
        val address = state.value.printerBluetoothAddress
        val version = scanSessionVersion
        val accountVersion = selectionPrintAccountVersion
        // The scans are already committed. Finish these labels even if the operator changes jobs.
        val batch = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                prior?.join()
                val printedAny = printSelectionStickers(jobId, jobName, work, address, accountVersion)
                if (printedAny && accountVersion == selectionPrintAccountVersion && version == scanSessionVersion) {
                    _state.update {
                        if (it.selectedScanJob?.id == jobId && it.printerError == null &&
                            it.selectionPrintFailures.keys.none { key -> key.jobId == jobId }
                        ) it.copy(printerMessage = R.string.printer_printed) else it
                    }
                }
            } finally {
                if (selectionPrintJob === coroutineContext[Job]) {
                    selectionPrintJob = null
                    _state.update { it.copy(selectionPrinterLoading = false) }
                }
            }
        }
        selectionPrintJob = batch
        _state.update { it.copy(selectionPrinterLoading = true) }
        batch.start()
    }

    private suspend fun printSelectionStickers(
        jobId: Int,
        jobName: String,
        work: List<Pair<Int, SelectionPrintWork>>,
        address: String?,
        accountVersion: Int
    ): Boolean {
        val app = getApplication<Application>()
        var printedAny = false
        for ((parcelId, operation) in work) {
            if (accountVersion != selectionPrintAccountVersion) return false
            val error = try {
                when (operation) {
                    is SelectionPrintWork.Invalid -> operation.error
                    is SelectionPrintWork.Kgt -> printerResultError(stickerPrintService.print(address, operation.code))
                    is SelectionPrintWork.Export ->
                        printerResultError(stickerPrintService.printTajikistanExportSticker(address, operation.sticker))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.e(javaClass.simpleName, "Could not print selected parcel $parcelId", failure)
                app.getString(R.string.printer_print_failed, app.getString(R.string.error_unknown))
            }
            if (accountVersion != selectionPrintAccountVersion) return false
            if (error == null) printedAny = true
            val key = SelectionPrintKey(jobId, parcelId)
            _state.update {
                val failures = if (error == null) {
                    it.selectionPrintFailures - key
                } else {
                    it.selectionPrintFailures + (key to SelectionPrintFailure(jobName, operation, error))
                }
                it.copy(selectionPrintFailures = failures)
            }
        }
        return printedAny
    }

    fun logout() {
        viewModelScope.launch {
            if (signingOut) return@launch
            signingOut = true
            selectionPrintAccountVersion += 1
            selectionPrintJob?.cancel()
            selectionPrintJob = null
            _state.update { it.copy(selectionPrinterLoading = false, selectionPrintFailures = emptyMap()) }
            cancelPrinterTest()
            cancelMonitorStickerPrint()
            updatePrinterSession()
            try {
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
                    MainState(
                        email = it.email,
                        password = "",
                        externalScannerEnabled = it.externalScannerEnabled,
                        printerAutoPrintEnabled = it.printerAutoPrintEnabled,
                        kgtVoiceEnabled = it.kgtVoiceEnabled,
                        relabelingSubmode = it.relabelingSubmode,
                        printerBluetoothAddress = it.printerBluetoothAddress,
                        bondedPrinters = it.bondedPrinters
                    )
                }
            } finally {
                signingOut = false
                updatePrinterSession()
            }
        }
    }

    private fun followLocalScanResult(result: ScanResultItem) {
        when (val action = localScanFollowAction(state.value.monitorAutoFollow, result.followTarget)) {
            LocalScanFollowAction.None -> Unit
            LocalScanFollowAction.OpenRegister -> openMonitorRegister()
            is LocalScanFollowAction.OpenDetail -> loadMonitorDetail(
                action.scope,
                highlightedParcelId = action.highlightedParcelId
            )
        }
    }

    private suspend fun printKgtStickerInternal(code: String) {
        val stickerCode = kgtStickerCode(code)
        if (stickerCode == null) {
            _state.update {
                it.copy(
                    printerError = getApplication<Application>().getString(R.string.printer_invalid_sticker),
                    printerMessage = null
                )
            }
            return
        }

        _state.update { it.copy(printerLoading = true, printerError = null, printerMessage = null) }
        val jobId = state.value.selectedScanJob?.id
        val accountVersion = selectionPrintAccountVersion
        try {
            val result = stickerPrintService.print(state.value.printerBluetoothAddress, stickerCode)
            if (accountVersion != selectionPrintAccountVersion) return
            if (result == KgtStickerPrintResult.Success && jobId != null) {
                _state.update { it.copy(selectionPrintFailures = it.selectionPrintFailures.afterKgtReprint(jobId, stickerCode)) }
            }
            applyPrinterResult(result)
        } finally {
            if (accountVersion == selectionPrintAccountVersion) _state.update { it.copy(printerLoading = false) }
        }
    }

    private suspend fun printTajikistanExportStickerInternal(sticker: TajikistanExportSticker) {
        _state.update { it.copy(printerLoading = true, printerError = null, printerMessage = null) }
        try {
            val result = stickerPrintService.printTajikistanExportSticker(
                state.value.printerBluetoothAddress,
                sticker
            )
            applyPrinterResult(result)
        } finally {
            _state.update { it.copy(printerLoading = false) }
        }
    }

    private suspend fun printParcelStickerInternal(sticker: ParcelSticker) {
        _state.update { it.copy(printerLoading = true, printerError = null, printerMessage = null) }
        try {
            val result = stickerPrintService.printParcelSticker(
                state.value.printerBluetoothAddress,
                sticker
            )
            applyPrinterResult(result)
        } finally {
            _state.update { it.copy(printerLoading = false) }
        }
    }

    private fun applyPrinterResult(result: KgtStickerPrintResult) {
        if (result == KgtStickerPrintResult.PermissionMissing) {
            setPrinterPermissionDenied()
            return
        }
        val error = printerResultError(result)
        _state.update {
            it.copy(printerError = error, printerMessage = R.string.printer_printed.takeIf { error == null })
        }
    }

    private fun printerResultError(result: KgtStickerPrintResult): String? {
        val app = getApplication<Application>()
        return when (result) {
            KgtStickerPrintResult.Success -> null
            KgtStickerPrintResult.MissingPrinter -> app.getString(R.string.printer_select_required)
            KgtStickerPrintResult.PermissionMissing -> app.getString(R.string.printer_permission_missing)
            is KgtStickerPrintResult.PrinterNotFound -> app.getString(R.string.printer_not_found)
            is KgtStickerPrintResult.InvalidSticker -> app.getString(
                if (result.contentOverflow) R.string.printer_tj_sticker_overflow else R.string.printer_invalid_sticker
            )
            is KgtStickerPrintResult.Failed -> app.getString(
                R.string.printer_print_failed,
                result.message?.takeIf(String::isNotBlank) ?: app.getString(R.string.error_unknown)
            )
        }
    }

    private fun selectedPrinterMissingMessage(
        printers: List<BluetoothPrinterDevice>,
        selectedAddress: String?
    ): String? {
        if (selectedAddress.isNullOrBlank()) {
            return null
        }
        return if (printers.any { it.address == selectedAddress }) {
            null
        } else {
            getApplication<Application>().getString(R.string.printer_not_found)
        }
    }
}

private const val SCAN_JOB_LIST_REFRESH_DEBOUNCE_MS = 300L
