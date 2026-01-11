package com.example.mt93scanner.net

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("/login")
    suspend fun login(@Body req: Credentials): UserViewItemWithJWT

    @POST("/scan/check")
    suspend fun checkCode(
        @Header("Authorization") bearer: String,
        @Body req: ScanCheckRequest
    ): ScanCheckResponse
}
