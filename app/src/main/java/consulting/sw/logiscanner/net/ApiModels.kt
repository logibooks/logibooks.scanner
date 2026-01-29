// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.net

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Credentials(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class ErrMessage(
    val msg: String
)

@JsonClass(generateAdapter = true)
data class UserViewItemWithJWT(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val email: String,
    val roles: List<String>,
    val token: String
)

@JsonClass(generateAdapter = true)
data class ScanJob(
    val id: Int,
    val name: String,
    val description: String?,
    val status: String,
    val scanJobType: String
)

@JsonClass(generateAdapter = true)
data class ScanRequest(
    val scanJobId: Int,
    val code: String
)

@JsonClass(generateAdapter = true)
data class ScanResponse(
    val count: Int
)

@JsonClass(generateAdapter = true)
data class ScanJobOps(
    val scanJobTypes: Map<String, String>,
    val scanJobStatuses: Map<String, String>
)
