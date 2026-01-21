package com.example.logiscanner.repo

import com.example.logiscanner.net.NetworkModule
import com.example.logiscanner.net.ScanCheckRequest

class ScanRepository(baseUrl: String, private val token: String) {

    private val api = NetworkModule.createApi(baseUrl)

    suspend fun check(code: String): Boolean {
        val res = api.checkCode("Bearer $token", ScanCheckRequest(code))
        return res.match
    }
}
