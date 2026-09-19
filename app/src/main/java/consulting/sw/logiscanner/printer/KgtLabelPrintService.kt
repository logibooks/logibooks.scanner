// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class KgtLabelPrintResult {
    object Success : KgtLabelPrintResult()
    object MissingPrinter : KgtLabelPrintResult()
    object PermissionMissing : KgtLabelPrintResult()
    data class PrinterNotFound(val address: String) : KgtLabelPrintResult()
    data class InvalidLabel(
        val message: String,
        val contentOverflow: Boolean = false
    ) : KgtLabelPrintResult()
    data class Failed(val message: String?) : KgtLabelPrintResult()
}

class KgtLabelPrintService(
    private val renderer: TscLabelRenderer,
    private val client: LabelPrinterClient,
    private val tajikistanRenderer: TajikistanExportLabelRenderer = TajikistanExportLabelRenderer(),
    private val parcelLabelRenderer: ParcelLabelRenderer = ParcelLabelRenderer()
) {
    private val mutex = Mutex()

    suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = client.listBondedPrinters()

    suspend fun print(address: String?, code: String): KgtLabelPrintResult {
        return printRendered(address) {
            renderer.render(code)
        }
    }

    suspend fun printFullRelabeling(address: String?, parcelId: Int, registerId: Int): KgtLabelPrintResult {
        return printRendered(address) {
            renderer.renderFullRelabeling(parcelId, registerId)
        }
    }

    suspend fun printTajikistanExport(
        address: String?,
        label: TajikistanExportLabel
    ): KgtLabelPrintResult {
        return printRendered(address) {
            tajikistanRenderer.render(label)
        }
    }

    suspend fun printParcelLabel(address: String?, label: ParcelLabel): KgtLabelPrintResult {
        return printRendered(address) {
            parcelLabelRenderer.render(label)
        }
    }

    private suspend fun printRendered(
        address: String?,
        renderPayload: () -> ByteArray
    ): KgtLabelPrintResult {
        if (address.isNullOrBlank()) {
            return KgtLabelPrintResult.MissingPrinter
        }

        return mutex.withLock {
            try {
                client.print(address, renderPayload())
                KgtLabelPrintResult.Success
            } catch (ex: CancellationException) {
                throw ex
            } catch (_: PrinterPermissionMissingException) {
                KgtLabelPrintResult.PermissionMissing
            } catch (ex: PrinterNotFoundException) {
                KgtLabelPrintResult.PrinterNotFound(ex.address)
            } catch (ex: TajikistanLabelOverflowException) {
                KgtLabelPrintResult.InvalidLabel(ex.message.orEmpty(), contentOverflow = true)
            } catch (ex: IllegalArgumentException) {
                KgtLabelPrintResult.InvalidLabel(ex.message.orEmpty())
            } catch (ex: Exception) {
                KgtLabelPrintResult.Failed(ex.message)
            }
        }
    }
}
