// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class KgtStickerPrintResult {
    object Success : KgtStickerPrintResult()
    object MissingPrinter : KgtStickerPrintResult()
    object PermissionMissing : KgtStickerPrintResult()
    data class PrinterNotFound(val address: String) : KgtStickerPrintResult()
    data class InvalidSticker(
        val message: String,
        val contentOverflow: Boolean = false
    ) : KgtStickerPrintResult()
    data class Failed(val message: String?) : KgtStickerPrintResult()
}

class KgtStickerPrintService(
    private val renderer: TscStickerRenderer,
    private val client: StickerPrinterClient,
    private val tajikistanRenderer: TajikistanExportStickerRenderer = TajikistanExportStickerRenderer(),
    private val parcelStickerRenderer: ParcelStickerRenderer = ParcelStickerRenderer()
) {
    private val mutex = Mutex()

    suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = client.listBondedPrinters()

    suspend fun print(address: String?, code: String): KgtStickerPrintResult {
        return printRendered(address) {
            renderer.render(code)
        }
    }

    suspend fun printFullRelabeling(address: String?, parcelId: Int, registerId: Int): KgtStickerPrintResult {
        return printRendered(address) {
            renderer.renderFullRelabeling(parcelId, registerId)
        }
    }

    suspend fun printTajikistanExportSticker(
        address: String?,
        sticker: TajikistanExportSticker
    ): KgtStickerPrintResult {
        return printRendered(address) {
            tajikistanRenderer.render(sticker)
        }
    }

    suspend fun printParcelSticker(address: String?, sticker: ParcelSticker): KgtStickerPrintResult {
        return printRendered(address) {
            parcelStickerRenderer.render(sticker)
        }
    }

    private suspend fun printRendered(
        address: String?,
        renderPayload: () -> ByteArray
    ): KgtStickerPrintResult {
        if (address.isNullOrBlank()) {
            return KgtStickerPrintResult.MissingPrinter
        }

        return mutex.withLock {
            try {
                client.print(address, renderPayload())
                KgtStickerPrintResult.Success
            } catch (ex: CancellationException) {
                throw ex
            } catch (_: PrinterPermissionMissingException) {
                KgtStickerPrintResult.PermissionMissing
            } catch (ex: PrinterNotFoundException) {
                KgtStickerPrintResult.PrinterNotFound(ex.address)
            } catch (ex: TajikistanStickerOverflowException) {
                KgtStickerPrintResult.InvalidSticker(ex.message.orEmpty(), contentOverflow = true)
            } catch (ex: IllegalArgumentException) {
                KgtStickerPrintResult.InvalidSticker(ex.message.orEmpty())
            } catch (ex: Exception) {
                KgtStickerPrintResult.Failed(ex.message)
            }
        }
    }
}
