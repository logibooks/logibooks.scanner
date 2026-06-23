// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import java.util.Locale

internal fun isBluetoothPrinterCandidate(
    majorDeviceClass: Int?,
    deviceClass: Int?,
    name: String?
): Boolean {
    return isBluetoothPrinterClass(majorDeviceClass, deviceClass) || isKnownTscPrinterName(name)
}

internal fun isBluetoothPrinterClass(majorDeviceClass: Int?, deviceClass: Int?): Boolean {
    if (majorDeviceClass != BLUETOOTH_MAJOR_DEVICE_CLASS_IMAGING || deviceClass == null) {
        return false
    }

    return (deviceClass and BLUETOOTH_DEVICE_CLASS_IMAGING_PRINTER) == BLUETOOTH_DEVICE_CLASS_IMAGING_PRINTER
}

private fun isKnownTscPrinterName(name: String?): Boolean {
    val normalized = name
        ?.trim()
        ?.uppercase(Locale.ROOT)
        .orEmpty()
    return normalized.contains("TSC") || normalized.contains("RE310")
}

private const val BLUETOOTH_MAJOR_DEVICE_CLASS_IMAGING = 0x0600
private const val BLUETOOTH_DEVICE_CLASS_IMAGING_PRINTER = 0x0680
