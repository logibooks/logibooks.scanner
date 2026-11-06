// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import com.squareup.moshi.Moshi
import consulting.sw.logiscanner.net.ErrMessage
import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.BulkyItemsModes
import consulting.sw.logiscanner.net.ScanRequest
import consulting.sw.logiscanner.net.ScanResultItem
import retrofit2.HttpException

class AmbiguousReturnParcelStickerException(cause: HttpException) : Exception(cause)

class ScanRepository(baseUrl: String, private val token: String, onUnauthorized: (() -> Unit)? = null) {

    private val api = NetworkModule.createApi(baseUrl, onUnauthorized)
    private val errorAdapter = Moshi.Builder().build().adapter(ErrMessage::class.java)

    /** Sends one scan request with the selected bulky item mode. */
    suspend fun scan(
        scanJobId: Int,
        code: String,
        bulkyItemsMode: Int = BulkyItemsModes.OFF
    ): ScanResultItem {
        try {
            return api.scan("Bearer $token", ScanRequest(scanJobId, code, bulkyItemsMode))
        } catch (failure: HttpException) {
            if (failure.code() == 409) {
                val error = runCatching {
                    failure.response()?.errorBody()?.string()?.let(errorAdapter::fromJson)
                }.getOrNull()
                if (error?.code == "AmbiguousReturnParcelSticker") throw AmbiguousReturnParcelStickerException(failure)
            }
            throw failure
        }
    }
}
