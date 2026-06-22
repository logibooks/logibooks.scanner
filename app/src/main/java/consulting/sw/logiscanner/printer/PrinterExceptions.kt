// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

open class PrinterException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PrinterPermissionMissingException : PrinterException("Bluetooth printer permission is missing")

class PrinterNotFoundException(val address: String) : PrinterException("Printer was not found: $address")

class PrinterUnavailableException(message: String, cause: Throwable? = null) : PrinterException(message, cause)
