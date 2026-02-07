// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanRequest
import consulting.sw.logiscanner.net.ScanResultItem

class ScanRepository(baseUrl: String, private val token: String, onUnauthorized: (() -> Unit)? = null) {

    private val api = NetworkModule.createApi(baseUrl, onUnauthorized)

    suspend fun scan(scanJobId: Int, code: String): ScanResultItem {
        return api.scan("Bearer $token", ScanRequest(scanJobId, code))
    }
}
