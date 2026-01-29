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
class Mt93ScanReceiver(private val onScan: (String) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // As per MT93 docs, the data is in a string extra called "SCAN_BARCODE1"
        val result = intent.getStringExtra("SCAN_BARCODE1") ?: return

        onScan(result)
    }
}