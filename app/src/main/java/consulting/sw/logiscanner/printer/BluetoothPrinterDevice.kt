// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

data class BluetoothPrinterDevice(
    val name: String,
    val address: String
) {
    val displayName: String
        get() = if (name.isBlank()) address else "$name ($address)"
}
