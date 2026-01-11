package com.example.mt93scanner.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

data class ScanEvent(
    val barcode1: String?,
    val barcode2: String?,
    val barcodeType: Int,
    val state: String?
)

class Mt93ScanReceiver(
    private val onScan: (ScanEvent) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "nlscan.action.SCANNER_RESULT") return

        val event = ScanEvent(
            barcode1 = intent.getStringExtra("SCAN_BARCODE1"),
            barcode2 = intent.getStringExtra("SCAN_BARCODE2"),
            barcodeType = intent.getIntExtra("SCAN_BARCODE_TYPE", -1),
            state = intent.getStringExtra("SCAN_STATE")
        )
        onScan(event)
    }
}
