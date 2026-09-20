// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.util.Locale

class ParcelStickerRenderer {

    fun render(sticker: ParcelSticker): ByteArray = when (sticker) {
        is ParcelSticker.WbrN -> renderWbrN(sticker)
        is ParcelSticker.Ozon -> renderOzon(sticker)
    }

    private fun renderWbrN(sticker: ParcelSticker.WbrN): ByteArray {
        val stickerNumber = normalizeText(sticker.sticker)
        val stickerCode = normalizeQr(sticker.stickerCode)
        require(stickerNumber != null || stickerCode != null) { "WbrN sticker has no printable data" }

        val bitmap = blankBitmap()
        val canvas = Canvas(bitmap)
        drawRotatedCentered(canvas, WBR_BRAND, 31f, 160f, -90f, brandPaint(42f))
        stickerNumber?.let { value ->
            val (first, second) = splitInHalf(value)
            drawRotatedCentered(canvas, first, 405f, 160f, -90f, textPaint(24f, bold = true))
            if (second.isNotEmpty()) {
                drawRotatedCentered(canvas, second, 438f, 160f, -90f, textPaint(24f, bold = true))
            }
        }

        val qrCommands = if (stickerCode == null) {
            emptyList()
        } else {
            listOf(
                qrCommand(169, 94, 6, stickerCode),
                qrCommand(75, 20, 3, stickerCode),
                qrCommand(321, 20, 3, stickerCode),
                qrCommand(75, 226, 3, stickerCode),
                qrCommand(321, 226, 3, stickerCode)
            )
        }
        return buildPayload(bitmap, qrCommands)
    }

    private fun renderOzon(sticker: ParcelSticker.Ozon): ByteArray {
        val postingNumber = normalizeText(sticker.postingNumber)
        val barcode = normalizeQr(sticker.barcode)
        val destinationCity = normalizeText(sticker.destinationCity)?.uppercase(RUSSIAN_LOCALE)
        require(postingNumber != null || barcode != null || destinationCity != null) {
            "Ozon sticker has no printable data"
        }

        val bitmap = blankBitmap()
        val canvas = Canvas(bitmap)
        drawRotatedCentered(canvas, OZON_BRAND, 31f, 160f, -90f, brandPaint(36f))
        destinationCity?.let { city ->
            drawRotatedFitted(canvas, city, 435f, 160f, -90f, maxLength = 270f)
        }
        postingNumber?.let { value ->
            val (first, second) = splitPostingNumber(value)
            drawRotatedCentered(canvas, first, 105f, 160f, -90f, textPaint(20f, bold = true))
            if (second.isNotEmpty()) {
                drawRotatedCentered(canvas, second, 350f, 160f, -90f, textPaint(20f, bold = true))
            }
        }

        val qrCommands = barcode?.let { listOf(qrCommand(169, 94, 6, it)) }.orEmpty()
        return buildPayload(bitmap, qrCommands)
    }

    private fun blankBitmap(): Bitmap = Bitmap.createBitmap(
        STICKER_WIDTH_DOTS,
        STICKER_HEIGHT_DOTS,
        Bitmap.Config.ARGB_8888
    ).also { it.eraseColor(Color.WHITE) }

    private fun drawRotatedFitted(
        canvas: Canvas,
        value: String,
        centerX: Float,
        centerY: Float,
        rotation: Float,
        maxLength: Float
    ) {
        var size = 27f
        var paint = textPaint(size, bold = true)
        while (size > 14f && paint.measureText(value) > maxLength) {
            size -= 1f
            paint = textPaint(size, bold = true)
        }
        drawRotatedCentered(canvas, value, centerX, centerY, rotation, paint)
    }

    private fun drawRotatedCentered(
        canvas: Canvas,
        value: String,
        centerX: Float,
        centerY: Float,
        rotation: Float,
        paint: Paint
    ) {
        canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        val baseline = centerY - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(value, centerX - paint.measureText(value) / 2f, baseline, paint)
        canvas.restore()
    }

    private fun textPaint(size: Float, bold: Boolean = false): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }

    private fun brandPaint(size: Float): Paint = textPaint(size, bold = true).apply {
        letterSpacing = 0.04f
    }

    private fun splitInHalf(value: String): Pair<String, String> {
        val midpoint = (value.length + 1) / 2
        return value.substring(0, midpoint) to value.substring(midpoint)
    }

    private fun splitPostingNumber(value: String): Pair<String, String> {
        val separator = value.indexOf('-').takeIf { it in 1 until value.lastIndex }
        return if (separator != null) {
            value.substring(0, separator) to value.substring(separator + 1)
        } else {
            splitInHalf(value)
        }
    }

    private fun normalizeText(value: String?): String? = value
        ?.trim()
        ?.takeIf { text -> text.isNotEmpty() && text.none { it.code < 0x20 || it.code == 0x7F } }

    private fun normalizeQr(value: String?): String? = normalizeText(value)
        ?.takeIf { '"' !in it && it.all { char -> char.code <= 0x7E } }

    private fun qrCommand(x: Int, y: Int, cellDots: Int, value: String): String =
        "QRCODE $x,$y,L,$cellDots,A,0,M2,S7,\"$value\""

    private fun buildPayload(bitmap: Bitmap, commands: List<String>): ByteArray {
        val raster = encodeMonochrome(bitmap)
        val output = ByteArrayOutputStream()
        listOf(
            "SIZE 58 mm,40 mm",
            "GAP 2 mm,0 mm",
            "DENSITY 8",
            "DIRECTION 1",
            "REFERENCE 0,0",
            "CLS"
        ).forEach { command -> output.write("$command\r\n".toByteArray(Charsets.US_ASCII)) }
        output.write("BITMAP 0,0,$RASTER_BYTES_PER_ROW,$STICKER_HEIGHT_DOTS,0,".toByteArray(Charsets.US_ASCII))
        output.write(raster)
        output.write("\r\n".toByteArray(Charsets.US_ASCII))
        commands.forEach { command -> output.write("$command\r\n".toByteArray(Charsets.US_ASCII)) }
        output.write("PRINT 1,1\r\n".toByteArray(Charsets.US_ASCII))
        return output.toByteArray()
    }

    private fun encodeMonochrome(bitmap: Bitmap): ByteArray {
        val pixels = IntArray(STICKER_WIDTH_DOTS)
        val raster = ByteArray(RASTER_BYTES_PER_ROW * STICKER_HEIGHT_DOTS)
        for (y in 0 until STICKER_HEIGHT_DOTS) {
            bitmap.getPixels(pixels, 0, STICKER_WIDTH_DOTS, 0, y, STICKER_WIDTH_DOTS, 1)
            for (x in 0 until STICKER_WIDTH_DOTS) {
                val pixel = pixels[x]
                val luminance = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                if (Color.alpha(pixel) >= 128 && luminance < 128) {
                    val index = y * RASTER_BYTES_PER_ROW + x / 8
                    raster[index] = (raster[index].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }
        return raster
    }

    companion object {
        const val STICKER_WIDTH_DOTS = 464
        const val STICKER_HEIGHT_DOTS = 320
        const val RASTER_BYTES_PER_ROW = STICKER_WIDTH_DOTS / 8
        const val RASTER_SIZE_BYTES = RASTER_BYTES_PER_ROW * STICKER_HEIGHT_DOTS

        private const val WBR_BRAND = "WB"
        private const val OZON_BRAND = "OZON"
        private val RUSSIAN_LOCALE = Locale.forLanguageTag("ru-RU")
    }
}
