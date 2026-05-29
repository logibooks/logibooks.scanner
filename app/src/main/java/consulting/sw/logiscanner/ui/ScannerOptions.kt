// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.R

fun scanHintResId(externalScannerEnabled: Boolean): Int {
    return if (externalScannerEnabled) {
        R.string.scan_hint_external_enabled
    } else {
        R.string.scan_hint_hardware_only
    }
}

fun hidScannerInputEnabled(externalScannerEnabled: Boolean): Boolean = externalScannerEnabled

fun hidFocusRecoverySuspended(
    externalScannerEnabled: Boolean,
    textFieldFocused: Boolean
): Boolean = externalScannerEnabled && textFieldFocused
