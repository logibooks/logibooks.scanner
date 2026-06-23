// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPrinterCapabilitiesTest {

    @Test
    fun printerImagingClassIsIncluded() {
        assertTrue(isBluetoothPrinterClass(IMAGING_MAJOR, IMAGING_PRINTER))
    }

    @Test
    fun nonPrinterClassesAreExcluded() {
        assertFalse(isBluetoothPrinterClass(IMAGING_MAJOR, IMAGING_SCANNER))
        assertFalse(isBluetoothPrinterClass(PERIPHERAL_MAJOR, PERIPHERAL_KEYBOARD))
        assertFalse(isBluetoothPrinterClass(PHONE_MAJOR, PHONE_UNCATEGORIZED))
        assertFalse(isBluetoothPrinterClass(AUDIO_VIDEO_MAJOR, AUDIO_VIDEO_UNCATEGORIZED))
        assertFalse(isBluetoothPrinterClass(IMAGING_MAJOR, IMAGING_UNCATEGORIZED))
        assertFalse(isBluetoothPrinterClass(null, IMAGING_PRINTER))
        assertFalse(isBluetoothPrinterClass(IMAGING_MAJOR, null))
    }

    private companion object {
        const val PHONE_MAJOR = 0x0200
        const val AUDIO_VIDEO_MAJOR = 0x0400
        const val PERIPHERAL_MAJOR = 0x0500
        const val IMAGING_MAJOR = 0x0600
        const val PHONE_UNCATEGORIZED = 0x0200
        const val AUDIO_VIDEO_UNCATEGORIZED = 0x0400
        const val PERIPHERAL_KEYBOARD = 0x0540
        const val IMAGING_UNCATEGORIZED = 0x0600
        const val IMAGING_SCANNER = 0x0640
        const val IMAGING_PRINTER = 0x0680
    }
}
