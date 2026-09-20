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
import kotlin.math.max

class TajikistanExportStickerRenderer {

    fun render(sticker: TajikistanExportSticker): ByteArray {
        val bitmap = Bitmap.createBitmap(STICKER_WIDTH_DOTS, STICKER_HEIGHT_DOTS, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawSticker(canvas, sticker)

        val raster = encodeMonochrome(bitmap)
        val barcode = code128SymbolWidthDots(sticker.orderNumber)
            ?.let { width -> barcodeCommand(sticker.orderNumber, width) }
        return buildPayload(raster, barcode)
    }

    private fun drawSticker(canvas: Canvas, sticker: TajikistanExportSticker) {
        val titlePaint = textPaint(TITLE_TEXT_DOTS, bold = true)
        val headerPaint = textPaint(HEADER_TEXT_DOTS, bold = true)
        val bodyPaint = textPaint(BODY_TEXT_DOTS)

        drawCentered(canvas, TITLE, 16f, titlePaint)
        drawCentered(canvas, "МЕСТ: ${sticker.placesCount ?: ""}", 31f, bodyPaint)

        val orderLine = "№ ЗАКАЗА ${sticker.orderNumber}"
        requireFits(orderLine, bodyPaint, STICKER_CONTENT_WIDTH)
        drawCentered(canvas, orderLine, 101f, bodyPaint)
        canvas.drawLine(4f, 106f, 460f, 106f, LINE_PAINT)

        val metadata = listOf(
            "ДАТА" to sticker.dispatchDate,
            "ВЕС" to sticker.weightKg?.let { "${decimal(it)} кг" }.orEmpty(),
            "ЦЕННОСТЬ" to sticker.declaredValue?.let { value ->
                listOf(decimal(value), sticker.currency).filter { it.isNotBlank() }.joinToString(" ")
            }.orEmpty(),
            "СЧЁТ" to sticker.accountNumber
        )
        metadata.forEachIndexed { index, (header, value) ->
            val x = METADATA_LEFT + index * METADATA_COLUMN_WIDTH
            canvas.drawText(header, x, 120f, bodyPaint)
            drawWrapped(
                canvas = canvas,
                text = value,
                x = x,
                firstBaseline = 134f,
                maxWidth = METADATA_TEXT_WIDTH,
                maxLines = 2,
                paint = bodyPaint
            )
        }
        canvas.drawLine(4f, 150f, 460f, 150f, LINE_PAINT)

        drawParty(
            canvas,
            header = "ОТПРАВИТЕЛЬ",
            name = sticker.senderName,
            address = sticker.senderAddress,
            left = 6f,
            headerPaint = headerPaint,
            bodyPaint = bodyPaint
        )
        drawParty(
            canvas,
            header = "ПОЛУЧАТЕЛЬ",
            name = sticker.recipientName,
            address = sticker.recipientAddress,
            phone = sticker.recipientPhone,
            left = 237f,
            headerPaint = headerPaint,
            bodyPaint = bodyPaint
        )
        canvas.drawLine(232f, 153f, 232f, 238f, LINE_PAINT)
        canvas.drawLine(4f, 241f, 460f, 241f, LINE_PAINT)

        canvas.drawText("ТОВАРЫ", 6f, 256f, headerPaint)
        val itemText = sticker.items.map { item ->
            listOfNotNull(
                item.description.takeIf { it.isNotBlank() },
                item.quantity?.toString()
            ).joinToString(" — ")
        }.filter { it.isNotBlank() }.joinToString("\n")
        drawWrapped(
            canvas = canvas,
            text = itemText,
            x = 6f,
            firstBaseline = 271f,
            maxWidth = 452f,
            maxLines = 4,
            paint = bodyPaint
        )
    }

    private fun drawParty(
        canvas: Canvas,
        header: String,
        name: String,
        address: String,
        phone: String? = null,
        left: Float,
        headerPaint: Paint,
        bodyPaint: Paint
    ) {
        canvas.drawText(header, left, 166f, headerPaint)
        val text = listOfNotNull(name, address, phone)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        drawWrapped(
            canvas = canvas,
            text = text,
            x = left,
            firstBaseline = 181f,
            maxWidth = 219f,
            maxLines = 5,
            paint = bodyPaint
        )
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        firstBaseline: Float,
        maxWidth: Float,
        maxLines: Int,
        paint: Paint
    ) {
        val lines = wrap(text, maxWidth, paint)
        if (lines.size > maxLines) {
            throw TajikistanStickerOverflowException()
        }
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, firstBaseline + index * BODY_LINE_HEIGHT, paint)
        }
    }

    private fun wrap(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        text.lines().forEach { paragraph ->
            if (paragraph.isBlank()) {
                lines += ""
                return@forEach
            }
            var current = ""
            paragraph.split(Regex("\\s+")).forEach { word ->
                val pieces = splitToFit(word, maxWidth, paint)
                pieces.forEach { piece ->
                    val candidate = if (current.isEmpty()) piece else "$current $piece"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = candidate
                    } else {
                        if (current.isNotEmpty()) {
                            lines += current
                        }
                        current = piece
                    }
                }
            }
            if (current.isNotEmpty()) {
                lines += current
            }
        }
        return lines
    }

    private fun splitToFit(word: String, maxWidth: Float, paint: Paint): List<String> {
        if (paint.measureText(word) <= maxWidth) {
            return listOf(word)
        }
        val result = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = start + 1
            while (end <= word.length && paint.measureText(word.substring(start, end)) <= maxWidth) {
                end += 1
            }
            val fittedEnd = max(start + 1, end - 1)
            result += word.substring(start, fittedEnd)
            start = fittedEnd
        }
        return result
    }

    private fun requireFits(text: String, paint: Paint, maxWidth: Float) {
        if (paint.measureText(text) > maxWidth) {
            throw TajikistanStickerOverflowException()
        }
    }

    private fun drawCentered(canvas: Canvas, text: String, baseline: Float, paint: Paint) {
        requireFits(text, paint, STICKER_CONTENT_WIDTH)
        val x = (STICKER_WIDTH_DOTS - paint.measureText(text)) / 2f
        canvas.drawText(text, x, baseline, paint)
    }

    private fun textPaint(size: Float, bold: Boolean = false): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }

    private fun decimal(value: Double): String = String.format(RUSSIAN_LOCALE, "%.2f", value)

    private fun barcodeCommand(value: String, width: Int): String {
        val x = (STICKER_WIDTH_DOTS - width) / 2
        return "BARCODE $x,$BARCODE_Y_DOTS,\"128\",$BARCODE_HEIGHT_DOTS,0,0," +
            "$BARCODE_NARROW_DOTS,$BARCODE_WIDE_DOTS,\"$value\""
    }

    private fun encodeMonochrome(bitmap: Bitmap): ByteArray {
        val pixels = IntArray(STICKER_WIDTH_DOTS)
        val raster = ByteArray(RASTER_BYTES_PER_ROW * STICKER_HEIGHT_DOTS)
        for (y in 0 until STICKER_HEIGHT_DOTS) {
            bitmap.getPixels(pixels, 0, STICKER_WIDTH_DOTS, 0, y, STICKER_WIDTH_DOTS, 1)
            for (x in 0 until STICKER_WIDTH_DOTS) {
                val pixel = pixels[x]
                val luminance = (
                    Color.red(pixel) * 299
                        + Color.green(pixel) * 587
                        + Color.blue(pixel) * 114
                    ) / 1000
                if (Color.alpha(pixel) >= 128 && luminance < 128) {
                    val index = y * RASTER_BYTES_PER_ROW + x / 8
                    raster[index] = (raster[index].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }
        return raster
    }

    private fun buildPayload(raster: ByteArray, barcode: String?): ByteArray {
        val output = ByteArrayOutputStream()
        listOf(
            "SIZE 58 mm,40 mm",
            "GAP 2 mm,0 mm",
            "DENSITY 8",
            "DIRECTION 1",
            "REFERENCE 0,0",
            "CLS"
        ).forEach { command ->
            output.write("$command\r\n".toByteArray(Charsets.US_ASCII))
        }
        output.write("BITMAP 0,0,$RASTER_BYTES_PER_ROW,$STICKER_HEIGHT_DOTS,0,".toByteArray(Charsets.US_ASCII))
        output.write(raster)
        output.write("\r\n".toByteArray(Charsets.US_ASCII))
        if (barcode != null) {
            output.write("$barcode\r\n".toByteArray(Charsets.US_ASCII))
        }
        output.write("PRINT 1,1\r\n".toByteArray(Charsets.US_ASCII))
        return output.toByteArray()
    }

    companion object {
        const val STICKER_WIDTH_DOTS = 464
        const val STICKER_HEIGHT_DOTS = 320
        const val RASTER_BYTES_PER_ROW = STICKER_WIDTH_DOTS / 8
        const val RASTER_SIZE_BYTES = RASTER_BYTES_PER_ROW * STICKER_HEIGHT_DOTS

        private const val TITLE = "МЕЖДУНАРОДНАЯ ТРАНСПОРТНАЯ НАКЛАДНАЯ"
        private const val TITLE_TEXT_DOTS = 15f
        private const val HEADER_TEXT_DOTS = 14f
        private const val BODY_TEXT_DOTS = 12f
        private const val BODY_LINE_HEIGHT = 13f
        private const val STICKER_CONTENT_WIDTH = 456f
        private const val METADATA_LEFT = 5f
        private const val METADATA_COLUMN_WIDTH = 114f
        private const val METADATA_TEXT_WIDTH = 109f
        private const val BARCODE_Y_DOTS = 37
        private const val BARCODE_HEIGHT_DOTS = 48
        private const val BARCODE_NARROW_DOTS = 2
        private const val BARCODE_WIDE_DOTS = 2
        private val RUSSIAN_LOCALE = Locale.forLanguageTag("ru-RU")
        private val LINE_PAINT = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }
    }
}

class TajikistanStickerOverflowException : IllegalArgumentException("Sticker content does not fit")
