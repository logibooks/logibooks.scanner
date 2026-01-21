// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanCheckRequest

class ScanRepository(baseUrl: String, private val token: String) {

    private val api = NetworkModule.createApi(baseUrl)

    suspend fun check(code: String): Boolean {
        val res = api.checkCode("Bearer $token", ScanCheckRequest(code))
        return res.match
    }
}
