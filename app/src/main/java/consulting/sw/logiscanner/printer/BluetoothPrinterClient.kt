// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

internal class BluetoothPrinterClient(
    private val context: Context,
    scope: CoroutineScope
) : StickerPrinterClient {
    private val session = PrinterSession(scope, Dispatchers.IO, ::openSocket)

    internal fun selectPrinter(printer: PrinterSessionTarget?) = session.select(printer)

    internal fun warmUp(printer: AutoPrintPrinter) = session.warmUp(printer)

    @SuppressLint("MissingPermission")
    override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = withContext(Dispatchers.IO) {
        ensureBluetoothConnectPermission()
        adapter().bondedDevices
            .orEmpty()
            .filter { device ->
                isBluetoothPrinterCandidate(
                    majorDeviceClass = device.bluetoothClass?.majorDeviceClass,
                    deviceClass = device.bluetoothClass?.deviceClass,
                    name = device.name
                )
            }
            .map { device ->
                BluetoothPrinterDevice(
                    name = device.name.orEmpty(),
                    address = device.address
                )
            }
            .sortedWith(compareBy<BluetoothPrinterDevice> { it.name.lowercase() }.thenBy { it.address })
    }

    override suspend fun print(address: String, payload: ByteArray) = preparePrint(address)(payload)

    override suspend fun printTest(
        address: String,
        payload: ByteArray,
        timing: PrinterTestTiming
    ) = session.printTest(address, payload, timing)

    override fun preparePrint(address: String): suspend (ByteArray) -> Unit {
        val send = session.preparePrint(address)
        return { payload ->
            try {
                ensureBluetoothConnectPermission()
                ensureBluetoothScanPermission()
                send(payload)
            } catch (ex: IOException) {
                throw PrinterUnavailableException("Unable to print sticker", ex)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(address: String): PrinterConnection {
        ensureBluetoothConnectPermission()
        ensureBluetoothScanPermission()
        val bluetoothAdapter = adapter()
        val device = bluetoothAdapter.bondedDevices.orEmpty().firstOrNull { it.address == address }
            ?: throw PrinterNotFoundException(address)

        try {
            bluetoothAdapter.cancelDiscovery()
        } catch (_: SecurityException) {
            // Discovery cancellation only improves connection speed; printing can continue without it.
        }
        return RfcommConnection(device.createRfcommSocketToServiceRecord(SPP_UUID))
    }

    private fun ensureBluetoothConnectPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            throw PrinterPermissionMissingException()
        }
    }

    private fun ensureBluetoothScanPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            throw PrinterPermissionMissingException()
        }
    }

    @Suppress("DEPRECATION")
    private fun adapter(): BluetoothAdapter {
        return BluetoothAdapter.getDefaultAdapter()
            ?: throw PrinterUnavailableException("Bluetooth is not available")
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

private class RfcommConnection(private val socket: BluetoothSocket) : PrinterConnection {
    @SuppressLint("MissingPermission") // Checked before the socket is created.
    override suspend fun connect() = socket.connect()

    override suspend fun write(payload: ByteArray) {
        val output = socket.outputStream
        output.write(payload)
        output.flush()
    }

    override suspend fun awaitDisconnect() {
        val input = socket.inputStream
        val buffer = ByteArray(256)
        while (input.read(buffer) != -1) {
            // Drain unsolicited printer data so EOF or a read failure can signal disconnect.
        }
    }

    override fun close() = socket.close()
}
