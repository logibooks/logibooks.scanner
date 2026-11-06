// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import consulting.sw.logiscanner.net.RecoveryBoxAdvice
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.ui.LocalScanResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReturnRecoveryFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun boxAdviceShowsOriginalBoxInstructionAndEveryCandidate() {
        val instruction = "Откройте коробку и проверьте посылки"
        render(advice = RecoveryBoxAdvice("ORIGINAL-BOX", listOf("P-001", "P-002"), instruction))
        compose.onNodeWithText("ORIGINAL-BOX").assertIsDisplayed()
        compose.onNodeWithText(instruction).assertIsDisplayed()
        compose.onNodeWithText("P-001").assertIsDisplayed()
        compose.onNodeWithText("P-002").assertIsDisplayed()
        compose.onNodeWithText(recoveryConfirmation()).assertDoesNotExist()
    }

    @Test
    fun individualRecoveryShowsItsOwnConfirmation() {
        render(confirmed = true)
        compose.onNodeWithText("P-003").assertIsDisplayed()
        compose.onNodeWithText(recoveryConfirmation()).assertIsDisplayed()
        compose.onNodeWithText("Откройте коробку и проверьте посылки").assertDoesNotExist()
    }

    private fun render(advice: RecoveryBoxAdvice? = null, confirmed: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                LocalScanResult(
                    lastCode = if (advice == null) "STICKER-P3" else "STICKER-BOX",
                    lastParcelCount = if (confirmed) 1 else 0,
                    lastBoxCount = if (advice == null) 0 else 1,
                    lastScanSource = if (advice == null) ScannedItemSources.PARCEL_STICKER else ScannedItemSources.BOX_STICKER,
                    lastItemNumbers = listOf(advice?.boxNumber ?: "P-003"),
                    lastRecoveryBoxAdvice = advice,
                    lastRecoveryConfirmed = confirmed,
                    lastExtData = null,
                    lastExtId = null,
                    lastTajikistanExportSticker = null,
                    lastScanTime = null,
                    printerSelected = false,
                    printerLoading = false,
                    onPrintKgtSticker = {},
                    snapshot = null,
                    detailSnapshot = null,
                    onPrintBoxSticker = {}
                )
            }
        }
    }

    private fun recoveryConfirmation(): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.monitor_recovery_confirmed)
}
