// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.content.res.Configuration
import consulting.sw.logiscanner.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StickerResourcesTest {

    @Test
    fun englishUsesSharedStickerWording() {
        assertEquals("Sticker", localizedString("en", R.string.sticker))
        assertEquals("Print sticker", localizedString("en", R.string.printer_print_sticker))

        stickerResourceIds.forEach { resourceId ->
            assertFalse(localizedString("en", resourceId).contains("label", ignoreCase = true))
        }
    }

    @Test
    fun russianUsesStickerWording() {
        assertEquals("Стикер", localizedString("ru", R.string.sticker))
        assertEquals("Напечатать стикер", localizedString("ru", R.string.printer_print_sticker))

        stickerResourceIds.forEach { resourceId ->
            val value = localizedString("ru", resourceId)
            assertFalse(value.contains("наклейк", ignoreCase = true))
            assertFalse(value.contains("этикетк", ignoreCase = true))
        }
    }

    private fun localizedString(languageTag: String, resourceId: Int): String {
        val application = RuntimeEnvironment.getApplication()
        val configuration = Configuration(application.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return application.createConfigurationContext(configuration).getString(resourceId)
    }

    private companion object {
        val stickerResourceIds = listOf(
            R.string.sticker,
            R.string.kgt_auto_print_sticker,
            R.string.printer_print_sticker,
            R.string.printer_invalid_sticker,
            R.string.printer_printed,
            R.string.printer_print_failed,
            R.string.printer_repeat_sticker,
            R.string.printer_tj_sticker_missing_data,
            R.string.printer_tj_sticker_overflow,
            R.string.monitor_parcel_gtc_code,
            R.string.monitor_parcel_scanned_sticker,
            R.string.monitor_parcel_wb_sticker,
            R.string.monitor_parcel_seller_sticker,
            R.string.monitor_parcel_sticker_code
        )
    }
}
