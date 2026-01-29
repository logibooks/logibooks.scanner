// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("api/Auth/login")
    suspend fun login(@Body req: Credentials): UserViewItemWithJWT

    @GET("api/ScanJobs/ops")
    suspend fun getOps(
        @Header("Authorization") bearer: String
    ): ScanJobOps

    @GET("api/ScanJobs/in-progress")
    suspend fun getInProgressJobs(
        @Header("Authorization") bearer: String
    ): List<ScanJob>

    @POST("api/ScanJobs/scan")
    suspend fun scan(
        @Header("Authorization") bearer: String,
        @Body req: ScanRequest
    ): ScanResponse
}
