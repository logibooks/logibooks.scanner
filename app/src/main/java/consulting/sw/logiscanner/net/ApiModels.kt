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
    val type: String
)

@JsonClass(generateAdapter = true)
data class ScanRequest(
    val id: Int,
    val code: String
)

@JsonClass(generateAdapter = true)
data class ScanResultItem(
    val count: Int,
    val extData: String?,
    val hasIssues: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ScanJobOpsItemDto(
    val value: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class ScanJobOps(
    val types: List<ScanJobOpsItemDto>,
    val operations: List<ScanJobOpsItemDto>,
    val modes: List<ScanJobOpsItemDto>,
    val statuses: List<ScanJobOpsItemDto>
)
