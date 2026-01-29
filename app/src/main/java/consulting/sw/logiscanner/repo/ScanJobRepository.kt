// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.ScanJob
import consulting.sw.logiscanner.net.ScanJobOps
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ScanJobRepository(baseUrl: String, private val token: String) {

    private val api = NetworkModule.createApi(baseUrl)
    private var ops: ScanJobOps? = null
    private val opsMutex = Mutex()

    suspend fun getOps(): ScanJobOps {
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
            // Try to match by value (if typeKey is numeric) or by name
            val typeItem = ops?.types?.find { 
                it.value.toString() == typeKey || it.name == typeKey 
            }
            typeItem?.name ?: typeKey
        }
    }
}
