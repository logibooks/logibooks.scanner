// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

// Deserialized from JSON sent by scanner
@JsonClass(generateAdapter = true)
data class ScannerResult(
    val barcode1: String?,
    val barcodeType: Int?,
    val state: String?
)

class Mt93ScanReceiver(private val onScan: (ScannerResult) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // As per MT93 docs, the data is in a string extra called "SCANNER_DECODE_DATA"
        val extra = intent.getStringExtra("SCANNER_DECODE_DATA") ?: return

        // The data is a JSON string, which we can deserialize with Moshi
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(ScannerResult::class.java)
        val result = adapter.fromJson(extra) ?: return

        onScan(result)
    }
}