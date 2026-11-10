// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

interface StickerPrinterClient {
    suspend fun listBondedPrinters(): List<BluetoothPrinterDevice>

    suspend fun print(address: String, payload: ByteArray)

    /** Binds the destination and connection session before the request waits in the print queue. */
    fun preparePrint(address: String): suspend (ByteArray) -> Unit = { print(address, it) }
}
