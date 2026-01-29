// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanRequest

class ScanRepository(baseUrl: String, private val token: String) {

    private val api = NetworkModule.createApi(baseUrl, handleUnauthorized = true)

    suspend fun scan(scanJobId: Int, code: String): Int {
        val res = api.scan("Bearer $token", ScanRequest(scanJobId, code))
        return res.count
    }
}
