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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothPrinterClient(
    private val context: Context
) : LabelPrinterClient {
    private val mutex = Mutex()

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

    @SuppressLint("MissingPermission")
    override suspend fun print(address: String, payload: ByteArray) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureBluetoothConnectPermission()
                ensureBluetoothScanPermission()
                val bluetoothAdapter = adapter()
                val device = bluetoothAdapter.bondedDevices
                    .orEmpty()
                    .firstOrNull { it.address == address }
                    ?: throw PrinterNotFoundException(address)

                try {
                    bluetoothAdapter.cancelDiscovery()
                } catch (_: SecurityException) {
                    // Discovery cancellation only improves connection speed; printing can continue without it.
                }
                var socket: BluetoothSocket? = null
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    socket.connect()
                    socket.outputStream.use { output ->
                        output.write(payload)
                        output.flush()
                    }
                } catch (ex: IOException) {
                    throw PrinterUnavailableException("Unable to print label", ex)
                } finally {
                    try {
                        socket?.close()
                    } catch (_: IOException) {
                        // The print attempt is already complete; ignore close failures.
                    }
                }
            }
        }
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
