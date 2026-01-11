package com.example.mt93scanner.repo

import com.example.mt93scanner.net.ApiService
import com.example.mt93scanner.net.Credentials
import com.example.mt93scanner.net.ScanCheckRequest
import com.example.mt93scanner.net.UserViewItemWithJWT

class AppRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String): UserViewItemWithJWT {
        return api.login(Credentials(Email = email, Password = password))
    }

    suspend fun checkCode(token: String, code: String): Boolean {
        val resp = api.checkCode(
            bearer = "Bearer $token",
            req = ScanCheckRequest(code = code)
        )
        return resp.match
    }
}
