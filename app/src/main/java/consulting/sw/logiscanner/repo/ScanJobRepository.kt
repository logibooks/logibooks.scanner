// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobOpsDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ScanJobRepository(baseUrl: String, private val token: String) {

    private val api = NetworkModule.createApi(baseUrl)
    private var ops: ScanJobOpsDto? = null
    private val opsMutex = Mutex()

    suspend fun getOps(): ScanJobOpsDto {
        opsMutex.withLock {
            if (ops == null) {
                ops = api.getOps("Bearer $token")
            }
            return ops!!
        }
    }

    suspend fun getInProgressJobs(): List<ScanJob> {
        return api.getInProgressJobs("Bearer $token")
    }
    
    suspend fun getScanJobTypeDisplay(typeKey: String): String {
        return opsMutex.withLock {
            // Try to parse typeKey as an integer and find matching item by value
            val typeValue = typeKey.toIntOrNull()
            if (typeValue != null) {
                ops?.types?.find { it.value == typeValue }?.name ?: typeKey
            } else {
                typeKey
            }
        }
    }
}
