// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.ApiService
import consulting.sw.logiscanner.net.Credentials
import consulting.sw.logiscanner.net.ScanCheckRequest
import consulting.sw.logiscanner.net.UserViewItemWithJWT

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
