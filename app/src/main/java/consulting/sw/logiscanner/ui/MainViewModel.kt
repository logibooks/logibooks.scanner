// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import consulting.sw.logiscanner.repo.LoginRepository
import consulting.sw.logiscanner.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val baseUrl: String = "",
    val email: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val isBusy: Boolean = false,
    val displayName: String? = null,
    val lastCode: String? = null,
    val lastMatch: Boolean? = null,
    val lastBarcodeType: Int? = null,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private lateinit var loginRepo: LoginRepository
    private lateinit var scanRepo: ScanRepository

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
                scanRepo = ScanRepository(url, loginRepo.token!!)
                _state.update { it.copy(isLoggedIn = true) }
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Login failed", ex)
                _state.update { it.copy(error = ex.message ?: "Unknown error") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun onScanned(code: String, barcodeType: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            try {
                val res = scanRepo.check(code)
                _state.update { it.copy(lastCode = code, lastMatch = res, lastBarcodeType = barcodeType) }
            } catch (ex: Exception) {
                Log.e(javaClass.simpleName, "Scan check failed", ex)
                _state.update { it.copy(error = ex.message ?: "Unknown error") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginRepo.logout()
        }
    }
}
