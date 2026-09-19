// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

sealed interface ParcelLabel {
    data class WbrN(
        val sticker: String?,
        val stickerCode: String?
    ) : ParcelLabel

    data class Ozon(
        val postingNumber: String?,
        val barcode: String?,
        val destinationCity: String?
    ) : ParcelLabel
}

