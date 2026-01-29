// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.ApiService
import consulting.sw.logiscanner.net.Credentials
import consulting.sw.logiscanner.net.ScanRequest
import consulting.sw.logiscanner.net.UserViewItemWithJWT

class AppRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String): UserViewItemWithJWT {
        return api.login(Credentials(Email = email, Password = password))
    }

    suspend fun scan(token: String, scanJobId: Int, code: String): Int {
        val resp = api.scan(
            bearer = "Bearer $token",
            req = ScanRequest(scanJobId = scanJobId, code = code)
        )
        return resp.count
    }
}
