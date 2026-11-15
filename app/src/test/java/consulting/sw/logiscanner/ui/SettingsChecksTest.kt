// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.Manifest
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.ViewModelProvider
import consulting.sw.logiscanner.BuildConfig
import consulting.sw.logiscanner.MainActivity
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.printer.BluetoothPrinterDevice
import consulting.sw.logiscanner.printer.KgtStickerPrintService
import consulting.sw.logiscanner.printer.StickerPrinterClient
import consulting.sw.logiscanner.printer.TscStickerRenderer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowTextToSpeech
import org.robolectric.util.ReflectionHelpers
import java.io.IOException
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsChecksTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun printerCheckRequiresASelectionAndRetriesAfterFailure() {
        val sent = mutableListOf<Pair<String, ByteArray>>()
        var failure: Exception? = IOException("Technical Bluetooth failure")
        val finishPrint = CompletableDeferred<Unit>()
        val client = object : StickerPrinterClient {
            override suspend fun listBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()

            override suspend fun print(address: String, payload: ByteArray) {
                failure?.let { throw it }
                sent += address to payload
                finishPrint.await()
            }
        }
        val vm = openSettings()
        val state = ReflectionHelpers.getField<MutableStateFlow<MainState>>(vm, "_state")
        val printedMessage = compose.activity.getString(R.string.printer_printed)
        val printError = compose.activity.getString(R.string.settings_test_printer_failed)
        val printButton = compose.onNode(hasText("Принтер") and hasClickAction())

        compose.runOnIdle {
            shadowOf(compose.activity.application).grantPermissions(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            ReflectionHelpers.setField(vm, "stickerPrintService", KgtStickerPrintService(TscStickerRenderer(), client))
            state.value = state.value.copy(printerMessage = R.string.printer_printed)
            vm.requestPrinterTest(hasPermission = true)
        }
        printButton.assertIsNotEnabled()
        compose.onNodeWithText(printedMessage).assertDoesNotExist()

        compose.runOnIdle {
            vm.setPrinterBluetoothAddress("AA:BB:CC:DD:EE:FF")
            state.value = state.value.copy(printerMessage = R.string.printer_no_paired)
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            state.value.printerBluetoothAddress == "AA:BB:CC:DD:EE:FF"
        }
        compose.onNodeWithText(compose.activity.getString(R.string.printer_no_paired)).performScrollTo().assertIsDisplayed()
        printButton.performScrollTo().assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText(printError).assertIsDisplayed()
        compose.onNodeWithText("Technical Bluetooth failure", substring = true).assertDoesNotExist()

        compose.runOnIdle { failure = null }
        printButton.assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        printButton.assertIsNotEnabled()
        compose.onNodeWithText(printError).assertDoesNotExist()
        compose.onNodeWithText("Тестовая этикетка отправлена").assertDoesNotExist()
        compose.runOnIdle {
            vm.requestPrinterTest(hasPermission = true)
            val (address, payload) = sent.single()
            assertEquals("AA:BB:CC:DD:EE:FF", address)
            assertEquals(
                listOf(
                    "SIZE 58 mm,40 mm",
                    "GAP 2 mm,0 mm",
                    "DENSITY 8",
                    "DIRECTION 1",
                    "REFERENCE 0,0",
                    "CODEPAGE 1251",
                    "CLS",
                    "TEXT 232,128,\"3\",0,1,1,2,\"Logibooks Scanner ${BuildConfig.VERSION_NAME}\"",
                    "TEXT 232,168,\"3\",0,1,1,2,\"Проверка принтера\"",
                    "PRINT 1,1"
                ).joinToString("\r\n", postfix = "\r\n"),
                payload.toString(charset("windows-1251"))
            )
            finishPrint.complete(Unit)
        }
        printButton.assertIsEnabled()
        compose.onNodeWithText(printedMessage).assertDoesNotExist()
        compose.onNodeWithText("Тестовая этикетка отправлена").assertIsDisplayed()
        compose.onNodeWithText("Общее время", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp", shadows = [RecordingSpeech::class])
    fun voiceCheckUsesEnglishForTheHeadingAndRussianForThePhrase() {
        val vm = openSettings()
        val voiceButton = compose.onNode(hasText("Голос") and hasClickAction())
        val voiceError = compose.onNodeWithText(compose.activity.getString(R.string.settings_test_voice_failed))
        val speech = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()) as RecordingSpeech
        val russian = Locale.forLanguageTag("ru-RU")
        val russianVoice = Voice("ru-male-test", russian, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, emptySet())
        compose.runOnIdle { speech.onInitListener.onInit(TextToSpeech.ERROR) }

        val checksBounds = compose.onNodeWithText("Проверки").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val printButtonBounds = compose.onNode(hasText("Принтер") and hasClickAction()).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val voiceButtonBounds = voiceButton.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val printerBounds = compose.onNode(hasText("Принтер") and !hasClickAction()).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        assertTrue(checksBounds.bottom <= printButtonBounds.top)
        assertEquals(printButtonBounds.center.y, voiceButtonBounds.center.y, 1f)
        assertTrue(printButtonBounds.right <= voiceButtonBounds.left)
        assertTrue(voiceButtonBounds.bottom <= printerBounds.top)

        val kgt = compose.onNodeWithText("КГТ").performScrollTo().assertIsDisplayed().assertIsSelected()
        val voiceSetting = compose.onNodeWithText("С голосом").assertIsDisplayed().assertIsOff()
        val kgtBounds = kgt.fetchSemanticsNode().boundsInRoot
        val voiceBounds = voiceSetting.fetchSemanticsNode().boundsInRoot
        assertEquals(kgtBounds.center.y, voiceBounds.center.y, 1f)
        assertTrue(kgtBounds.right <= voiceBounds.left)
        compose.onNodeWithText("Только КГТ").assertDoesNotExist()
        compose.onNodeWithText("Настройки режима КГТ").assertDoesNotExist()

        voiceButton.performScrollTo().assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        voiceError.performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            ShadowTextToSpeech.addLanguageAvailability(russian)
            ShadowTextToSpeech.addVoice(russianVoice)
            speech.onInitListener.onInit(TextToSpeech.SUCCESS)
        }
        voiceButton.performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        voiceError.performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(speech.utterances.isEmpty())
            ShadowTextToSpeech.addLanguageAvailability(Locale.ENGLISH)
        }
        voiceButton.performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        voiceError.assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(false, vm.state.value.kgtVoiceEnabled)
            assertEquals(
                listOf(
                    Triple("Logibooks Scanner ${BuildConfig.VERSION_NAME}", Locale.ENGLISH, TextToSpeech.QUEUE_FLUSH),
                    Triple("Проверка голоса", russian, TextToSpeech.QUEUE_ADD)
                ),
                speech.utterances.map { Triple(it.text, it.locale, it.queueMode) }
            )
            assertEquals(russianVoice, speech.currentVoice)
            speech.utteranceProgressListener.onError(speech.utterances.first().id, TextToSpeech.ERROR_OUTPUT)
        }
        voiceError.performScrollTo().assertIsDisplayed()

        voiceButton.performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        voiceError.assertDoesNotExist()
        compose.runOnIdle {
            val utteranceId = speech.utterances.last().id
            vm.closeSettings()
            vm.openSettings()
            speech.utteranceProgressListener.onError(utteranceId, TextToSpeech.ERROR_OUTPUT)
        }
        voiceError.assertDoesNotExist()
    }

    private fun openSettings(): MainViewModel = compose.runOnIdle {
        val vm = ViewModelProvider(compose.activity)[MainViewModel::class.java]
        val state = ReflectionHelpers.getField<MutableStateFlow<MainState>>(vm, "_state")
        state.value = state.value.copy(isLoggedIn = true, printerBluetoothAddress = null, kgtVoiceEnabled = false)
        vm.openSettings()
        vm
    }

    @Implements(TextToSpeech::class)
    class RecordingSpeech : ShadowTextToSpeech() {
        data class Utterance(val text: String, val locale: Locale?, val queueMode: Int, val id: String)

        val utterances = mutableListOf<Utterance>()
        private var activeLocale: Locale? = null

        @Implementation
        override fun setLanguage(locale: Locale): Int {
            val result = super.setLanguage(locale)
            if (result >= TextToSpeech.LANG_AVAILABLE) activeLocale = locale
            return result
        }

        @Implementation
        override fun setVoice(voice: Voice): Int {
            val result = super.setVoice(voice)
            if (result == TextToSpeech.SUCCESS) activeLocale = voice.locale
            return result
        }

        @Implementation
        override fun speak(text: CharSequence, queueMode: Int, params: Bundle?, utteranceId: String): Int {
            utterances += Utterance(text.toString(), activeLocale, queueMode, utteranceId)
            return super.speak(text, queueMode, params, utteranceId)
        }
    }
}
