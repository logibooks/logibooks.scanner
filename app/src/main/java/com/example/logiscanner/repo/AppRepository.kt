package com.example.logiscanner.repo

import com.example.logiscanner.net.ApiService
import com.example.logiscanner.net.Credentials
import com.example.logiscanner.net.ScanCheckRequest
import com.example.logiscanner.net.UserViewItemWithJWT

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
