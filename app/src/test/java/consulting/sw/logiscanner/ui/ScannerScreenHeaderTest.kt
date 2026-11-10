// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.SCAN_JOB_STATUS_IN_PROGRESS
import consulting.sw.logiscanner.net.ScanJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScannerScreenHeaderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun longNameIsEllipsizedToTheLeftOfActionsOnEverySignedInScreen() {
        val displayName = "Александр Константинопольский ".repeat(4).trimEnd()
        val state = compose.runOnIdle {
            val vm = ViewModelProvider(compose.activity)[MainViewModel::class.java]
            ReflectionHelpers.getField<MutableStateFlow<MainState>>(vm, "_state").also {
                it.value = it.value.copy(isLoggedIn = true, displayName = displayName)
            }
        }

        assertHeader(displayName, R.string.select_scan_job, R.string.settings_open, R.string.refresh_jobs, R.string.logout)
        compose.runOnIdle {
            state.value = state.value.copy(
                isScanning = true,
                selectedScanJob = ScanJob(1, "Тестовое задание", null, SCAN_JOB_STATUS_IN_PROGRESS.toString(), "30")
            )
        }
        assertHeader(displayName, R.string.ready_to_scan, R.string.settings_open, R.string.back_to_jobs, R.string.logout)

        compose.runOnIdle { state.value = state.value.copy(settingsOpen = true) }
        assertHeader(displayName, R.string.settings_title, R.string.settings_back)
    }

    private fun assertHeader(displayName: String, titleId: Int, vararg actionIds: Int) {
        val name = compose.onNodeWithText(displayName).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        name.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        assertTrue(layouts.single().isLineEllipsized(0))

        val nameBounds = name.fetchSemanticsNode().boundsInRoot
        val titleBounds = compose.onNodeWithText(compose.activity.getString(titleId)).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        assertEquals(titleBounds.left, nameBounds.left, 1f)
        assertTrue(nameBounds.bottom <= titleBounds.top)
        for (actionId in actionIds) {
            val actionBounds = compose.onNodeWithContentDescription(compose.activity.getString(actionId))
                .assertIsDisplayed().assertIsEnabled().fetchSemanticsNode().boundsInRoot
            assertEquals(actionBounds.center.y, nameBounds.center.y, 1f)
            assertTrue(nameBounds.right < actionBounds.left)
        }
    }
}
