// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import consulting.sw.logiscanner.scan.HidScanCollector
import kotlinx.coroutines.delay

/**
 * Invisible Compose input field that captures HID keystrokes from Bluetooth scanners.
 * 
 * This component renders a tiny, invisible BasicTextField that stays focused while enabled.
 * It captures input via:
 * 1. BasicTextField value changes (primary path) - handles all characters with correct case and punctuation
 * 2. onPreviewKeyEvent for Enter/Tab terminators
 * 
 * The component forwards characters to HidScanCollector which applies heuristics
 * to distinguish scanner input from human typing.
 * 
 * @param enabled Whether to activate HID input capture
 * @param onScan Callback invoked when a valid scan is detected
 * @param modifier Optional modifier for layout
 */
@Composable
fun HidScanInput(
    enabled: Boolean,
    onScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textValue by remember { mutableStateOf("") }
    var previousText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    
    val collector = remember(enabled) {
        HidScanCollector(
            scope = scope,
            onScan = onScan
        )
    }

    // Request focus when enabled and continuously ensure focus is maintained
    LaunchedEffect(enabled) {
        if (enabled) {
            // Initial focus request with retry
            repeat(3) {
                try {
                    focusRequester.requestFocus()
                    keyboardController?.hide()
                } catch (e: IllegalStateException) {
                    // Focus request may fail if not yet laid out, retry after delay
                }
                delay(100)
            }
        }
    }
    
    // Periodic focus recovery - ensures HID input stays active
    LaunchedEffect(enabled, isFocused) {
        if (enabled && !isFocused) {
            // Small delay to avoid rapid re-focus cycles
            delay(200)
            try {
                focusRequester.requestFocus()
                keyboardController?.hide()
            } catch (e: IllegalStateException) {
                // Ignore - will retry on next composition
            }
        }
    }

    DisposableEffect(enabled) {
        onDispose {
            collector.reset()
        }
    }

    BasicTextField(
        value = textValue,
        onValueChange = { newValue ->
            // Detect new characters added by IME (fallback path)
            if (newValue.length > previousText.length) {
                val addedChars = newValue.substring(previousText.length)
                for (c in addedChars) {
                    if (c == '\n' || c == '\r' || c == '\t') {
                        collector.onTerminator()
                    } else if (c.isLetterOrDigit() || c in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ ") {
                        // Accept all printable ASCII characters (letters, digits, punctuation, space)
                        collector.onPrintableChar(c)
                    }
                }
            }
            previousText = newValue
            // Clear text periodically to prevent unbounded growth
            if (newValue.length > 100) {
                textValue = ""
                previousText = ""
            } else {
                textValue = newValue
            }
        },
        // Prevent soft keyboard from appearing for HID-only input
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.None,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions.Default,
        modifier = modifier
            .size(1.dp) // Tiny, essentially invisible
            .clearAndSetSemantics {} // Remove from accessibility tree to avoid TalkBack focus trap
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (enabled && focusState.isFocused) {
                     // Ensure keyboard is hidden when focus is gained
                     keyboardController?.hide()
                }
                // Removed the block that hides keyboard on focus loss to prevent
                // interfering with navigation to screens that require keyboard
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Enter -> {
                            collector.onTerminator()
                            true
                        }
                        Key.Tab -> {
                            collector.onTerminator()
                            true
                        }
                        else -> {
                            // Let the TextField handle the character input naturally
                            // This will preserve case sensitivity and special characters
                            false
                        }
                    }
                } else {
                    false
                }
            },
        textStyle = TextStyle(
            color = Color.Transparent,
            fontSize = 1.sp
        )
    )
}
