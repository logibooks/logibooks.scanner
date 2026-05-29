// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

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

    @GET("api/ScanJobs/{id}/monitor")
    suspend fun getScanJobMonitor(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int,
        @Query("area") area: Int,
        @Query("boxId") boxId: Int? = null,
        @Query("bucketIndex") bucketIndex: Int? = null
    ): ScanJobMonitorSnapshot

    @GET("api/ScanJobs/{id}/monitor/resolve")
    suspend fun resolveScanJobMonitorTarget(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int,
        @Query("number") number: String
    ): ScanJobMonitorTarget

    @POST("api/ScanJobs/scan")
    suspend fun scan(
        @Header("Authorization") bearer: String,
        @Body req: ScanRequest
    ): ScanResultItem
}
