// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import consulting.sw.logiscanner.net.ParcelScanChoice
import consulting.sw.logiscanner.net.ParcelScanSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ru-w400dp-h800dp")
class ParcelSelectionTest {
    @get:Rule val compose = createComposeRule()

    private val choice = ParcelScanChoice(2, "Кабель", "017", sortMessage = "В зону сортировки")
    private val other = ParcelScanChoice(3, "Зарядное устройство", "018", sortMessage = "Передайте на проверку")
    private var pending = mutableStateOf(ParcelSelectionState("LABEL", 0, ParcelScanSelection("id", listOf(choice, other))))

    @Test fun startsCheckedShowsDistinctBoxesAndAllowsSkipping() {
        var submitted: Set<Int>? = null
        compose.setContent {
            MaterialTheme {
                ParcelSelectionDialog(
                    pending.value,
                    onToggle = { pending.value = pending.value.toggle(it) },
                    onConfirm = { submitted = pending.value.checkedIds },
                    onSkip = { submitted = emptySet() },
                    onClose = {}
                )
            }
        }

        compose.onNodeWithText("Номер коробки: 017").assertIsDisplayed()
        compose.onNodeWithText("Передайте на проверку").assertIsDisplayed()
        compose.onNodeWithText("Подтвердить (2)").performClick()
        assertEquals(setOf(2, 3), submitted)
        compose.onNodeWithText("Зарядное устройство").performClick()
        compose.onNodeWithText("Подтвердить (1)").performClick()
        assertEquals(setOf(2), submitted)
        compose.onNodeWithText("Кабель").performClick()
        compose.onNodeWithText("Подтвердить (0)").assertIsNotEnabled()
        compose.onNodeWithText("Пропустить").performClick()
        assertEquals(emptySet<Int>(), submitted)
    }

    @Test fun boxNumbersOnlyShowForDistinctNamedBoxesAndSubmittedSelectionIsFrozen() {
        val sameBox = pending.value.copy(selection = ParcelScanSelection("id", listOf(choice, other.copy(boxNumber = "017"))))
        assertFalse(sameBox.showBoxNumbers)
        val missing = sameBox.copy(selection = ParcelScanSelection("id", listOf(choice, other.copy(boxNumber = null))))
        assertFalse(missing.showBoxNumbers)
        val submitted = pending.value.copy(submitting = true)
        assertEquals(submitted, submitted.toggle(3))
    }

    @Test
    @Config(qualifiers = "ru-w400dp-h300dp")
    fun staleSelectionShowsRescanErrorOnShortScreen() {
        val many = (1..40).map { ParcelScanChoice(it, "Товар $it") }
        val stale = ParcelSelectionState("LABEL", 0, ParcelScanSelection("id", many), stale = true)
        compose.setContent {
            MaterialTheme {
                ParcelSelectionDialog(stale, onToggle = {}, onConfirm = {}, onSkip = {}, onClose = {})
            }
        }
        compose.onNodeWithText("Не удалось завершить выбор. Отсканируйте штрихкод снова.").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").assertIsDisplayed()
    }

    @Test fun eachSelectedParcelKeepsItsMessageAndManualPrintAction() {
        val printed = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                LocalScanResult(
                    lastCode = "LABEL",
                    lastParcelCount = 2,
                    lastBoxCount = 0,
                    lastScanSource = null,
                    lastItemNumbers = emptyList(),
                    lastExtData = null,
                    lastSelectedParcels = listOf(
                        SelectedParcelScan(2, "Кабель", null, "K000001", null, "Передайте на проверку", true),
                        SelectedParcelScan(3, "Зарядное устройство", null, "K000002", null)
                    ),
                    lastExtId = "K000002",
                    lastTajikistanExportSticker = null,
                    lastScanTime = null,
                    printerSelected = true,
                    printerLoading = false,
                    onPrintKgtSticker = { printed += it },
                    snapshot = null,
                    detailSnapshot = null,
                    onPrintMonitorSticker = { _, _ -> }
                )
            }
        }
        compose.onNodeWithText("Передайте на проверку").assertIsDisplayed()
        compose.onNodeWithText("Кабель #2 · Требуется проверка").assertIsDisplayed()
        val buttons = compose.onAllNodesWithContentDescription("Напечатать стикер")
        buttons[0].performClick()
        buttons[1].performClick()
        assertEquals(listOf("K000001", "K000002"), printed)
    }

    @Test fun successfulReprintClearsOnlyThatStickersFailure() {
        val remaining = mapOf(
            1 to SelectionPrintFailure("K000001", "First failure"),
            2 to SelectionPrintFailure("K000002", "Second failure"),
            3 to SelectionPrintFailure(null, "Export failure")
        ).afterKgtReprint("K000001")
        assertEquals(setOf(2, 3), remaining.keys)
    }

    @Test fun aNewSelectionClearsOldScanResultsAndGatesSettings() {
        val old = MainState(
            lastCode = "OLD",
            lastSelectedParcels = listOf(SelectedParcelScan(2, "Кабель", null, "K000001", null)),
            lastExtId = "K000001"
        )
        val opened = openParcelSelectionState(old, pending.value)
        assertNull(opened.lastCode)
        assertNull(opened.lastExtId)
        assertTrue(opened.lastSelectedParcels.isEmpty())
        assertFalse(openSettingsState(opened).settingsOpen)
        assertTrue(openSettingsState(old).settingsOpen)
    }

    @Test fun voiceAndInputGatesHonorASelection() {
        assertEquals("Выберите товары", parcelSelectionSpeech(null, "Выберите товары"))
        assertEquals("Найдено. Выберите товары", parcelSelectionSpeech("Найдено", "Выберите товары"))
        assertTrue(isScannerKey(KeyEvent.KEYCODE_ENTER, 0))
        assertTrue(isScannerKey(KeyEvent.KEYCODE_TAB, 0))
        assertTrue(isScannerKey(KeyEvent.KEYCODE_A, 'A'.code))
        assertFalse(isScannerKey(KeyEvent.KEYCODE_DPAD_DOWN, 0))

        val input = ScanInputQueue<String>()
        assertTrue(input.begin("first"))
        assertFalse(input.begin("second"))
        assertEquals("second", input.finish())
        assertNull(input.finish())
        assertTrue(input.begin("shared"))
        assertFalse(input.begin("queued behind dialog"))
        assertEquals(listOf("queued behind dialog"), input.discardQueued())
        assertNull(input.finish())
        assertTrue(input.begin("after dialog"))
    }
}
