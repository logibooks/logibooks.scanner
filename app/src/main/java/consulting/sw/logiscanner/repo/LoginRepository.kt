// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import android.content.Context
import consulting.sw.logiscanner.net.Credentials
import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.UserViewItemWithJWT
import consulting.sw.logiscanner.store.AuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import java.io.IOException

class LoginRepository(context: Context) {

    private val _state = MutableStateFlow(UserViewItemWithJWT(-1, "", "", "", "", emptyList(), ""))
    val state = _state.asStateFlow()

    val token: String? get() = state.value.token.takeIf { it.isNotBlank() }

    private val store = AuthStore(context)

    init {
        runBlocking {
            val stored = store.get().first()
            if (stored.token?.isNotBlank() == true) {
                _state.update {
                    it.copy(
                        token = stored.token,
                        firstName = stored.firstName!!,
                        lastName = stored.lastName!!,
                        id = stored.id!!
                    )
                }
            }
        }
    }

    suspend fun login(baseUrl: String, email: String, pass: String) {
        val api = NetworkModule.createApi(baseUrl)
        try {
            val res = api.login(Credentials(email, pass))

            store.save(res.token, res.firstName, res.lastName, res.id)
            _state.update {
                it.copy(
                    token = res.token,
                    firstName = res.firstName,
                    lastName = res.lastName,
                    id = res.id
                )
            }
        } catch (e: IOException) {
            throw IOException("Login failed: Server at $baseUrl might be down or misconfigured. Original error: ${e.message}", e)
        }
    }

    suspend fun logout() {
        store.clear()
        _state.update {
            it.copy(token = "", firstName = "", lastName = "", id = -1)
        }
    }
}
