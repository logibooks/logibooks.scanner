// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

interface LabelPrinterClient {
    suspend fun listBondedPrinters(): List<BluetoothPrinterDevice>

    suspend fun print(address: String, payload: ByteArray)
}
