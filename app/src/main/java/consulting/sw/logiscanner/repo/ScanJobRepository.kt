// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobOps

class ScanJobRepository(baseUrl: String, private val token: String) {

    private val api = NetworkModule.createApi(baseUrl)
    private var ops: ScanJobOps? = null

    suspend fun getOps(): ScanJobOps {
        if (ops == null) {
            ops = api.getOps("Bearer $token")
        }
        return ops!!
    }

    suspend fun getInProgressJobs(): List<ScanJob> {
        return api.getInProgressJobs("Bearer $token")
    }
    
    fun getScanJobTypeDisplay(typeKey: String): String {
        return ops?.scanJobTypes?.get(typeKey) ?: typeKey
    }
}
