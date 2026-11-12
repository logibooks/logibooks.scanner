// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import android.util.Log
import consulting.sw.logiscanner.net.LabelPrintPayload
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
    data class Failed(val message: String?, val cause: Exception) : KgtStickerPrintResult()
}

internal class KgtStickerPrintService(
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

    suspend fun printTestPage(address: String, title: String): PrinterTestReport {
        val timing = PrinterTestTiming()
        return try {
            timing.withQueueLock(mutex) {
                val payload = timing.measure(PrinterTestStage.PREPARE) { renderer.renderTestPage(title) }
                client.printTest(address, payload, timing)
            }
            timing.report()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val error = when (failure) {
                is PrinterTestFailure -> failure.error
                is PrinterPermissionMissingException, is SecurityException -> PrinterTestError.PERMISSION
                is PrinterNotFoundException -> PrinterTestError.PRINTER_NOT_FOUND
                is PrinterUnavailableException -> PrinterTestError.CONNECTION
                else -> PrinterTestError.OTHER
            }
            if (failure !is PrinterTestFailure) Log.w("PrinterTest", "Printer test failed", failure)
            timing.report(error)
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
        if (sticker !is ParcelSticker.WbrN) {
            return KgtStickerPrintResult.InvalidSticker("Parcel sticker printing is available for WbrN only")
        }
        return printRendered(address) {
            parcelStickerRenderer.render(sticker)
        }
    }

    suspend fun printBoxSticker(
        address: String?,
        loadLabel: suspend () -> LabelPrintPayload
    ): KgtStickerPrintResult = printRendered(address) {
        loadLabel().decodeTspl("WBR_BOX")
    }

    suspend fun printTajikistanInvoice(address: String, payload: ByteArray): KgtStickerPrintResult =
        printRendered(address) { payload }

    private suspend fun printRendered(
        address: String?,
        renderPayload: suspend () -> ByteArray
    ): KgtStickerPrintResult {
        if (address.isNullOrBlank()) {
            return KgtStickerPrintResult.MissingPrinter
        }

        val send = client.preparePrint(address)
        return mutex.withLock {
            try {
                send(renderPayload())
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
                KgtStickerPrintResult.Failed(ex.message, ex)
            }
        }
    }
}
