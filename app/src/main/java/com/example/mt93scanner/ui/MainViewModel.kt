package com.example.mt93scanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mt93scanner.net.NetworkModule
import com.example.mt93scanner.repo.AppRepository
import com.example.mt93scanner.store.AuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiState(
    val baseUrl: String = "https://your.server.tld/",
    val email: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val displayName: String? = null,

    val isBusy: Boolean = false,
    val lastCode: String? = null,
    val lastMatch: Boolean? = null,
    val lastBarcodeType: Int? = null,
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val authStore = AuthStore(app.applicationContext)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private fun normalizedBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun repo(): AppRepository {
        val api = NetworkModule.createApi(normalizedBaseUrl(_state.value.baseUrl))
        return AppRepository(api)
    }

    init {
        viewModelScope.launch {
            val auth = authStore.authFlow.first()
            _state.value = _state.value.copy(
                isLoggedIn = !auth.token.isNullOrBlank(),
                displayName = auth.displayName
            )
        }
    }

    fun setBaseUrl(v: String) { _state.value = _state.value.copy(baseUrl = v) }
    fun setEmail(v: String) { _state.value = _state.value.copy(email = v) }
    fun setPassword(v: String) { _state.value = _state.value.copy(password = v) }

    fun login() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, error = null)
            try {
                val user = repo().login(_state.value.email, _state.value.password)

                val display = buildString {
                    append(user.LastName)
                    append(" ")
                    append(user.FirstName)
                    if (!user.Patronymic.isNullOrBlank()) {
                        append(" ")
                        append(user.Patronymic)
                    }
                }

                authStore.setAuth(token = user.Token, displayName = display, userId = user.Id)

                _state.value = _state.value.copy(
                    isLoggedIn = true,
                    displayName = display,
                    isBusy = false
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isBusy = false,
                    error = "Login failed: ${t.message ?: t.javaClass.simpleName}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authStore.clear()
            _state.value = _state.value.copy(
                isLoggedIn = false,
                displayName = null,
                lastCode = null,
                lastMatch = null,
                lastBarcodeType = null,
                error = null
            )
        }
    }

    fun onScanned(code: String, barcodeType: Int) {
        viewModelScope.launch {
            val auth = authStore.authFlow.first()
            val token = auth.token
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(error = "Not logged in.")
                return@launch
            }

            // Simple strategy: ignore scans while a request is in-flight
            if (_state.value.isBusy) return@launch

            _state.value = _state.value.copy(
                isBusy = true,
                error = null,
                lastCode = code,
                lastMatch = null,
                lastBarcodeType = barcodeType
            )

            try {
                val match = repo().checkCode(token, code)
                _state.value = _state.value.copy(isBusy = false, lastMatch = match)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isBusy = false,
                    error = "Check failed: ${t.message ?: t.javaClass.simpleName}"
                )
            }
        }
    }
}
