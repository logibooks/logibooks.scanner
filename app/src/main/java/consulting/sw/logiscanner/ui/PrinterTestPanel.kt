// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.printer.PrinterTestError
import consulting.sw.logiscanner.printer.PrinterTestReport
import consulting.sw.logiscanner.printer.PrinterTestStage

@Composable
internal fun PrinterTestPanel(
    running: Boolean,
    permissionDenied: Boolean,
    report: PrinterTestReport?
) {
    if (running) {
        PrinterStatusMessages(loading = true, message = stringResource(R.string.printer_test_running), error = null)
    }
    if (permissionDenied) {
        PrinterStatusMessages(loading = false, message = null, error = stringResource(R.string.printer_test_permission))
    }
    if (report != null) {
        val status = stringResource(report.status())
        val details = report.details(LocalResources.current)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (report.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(details, style = MaterialTheme.typography.bodySmall)
    }
}

private fun PrinterTestReport.details(resources: Resources): String = buildString {
    for (stage in PrinterTestStage.entries) {
        val elapsed = elapsedMs[stage] ?: continue
        val format = if (stage == failedStage) R.string.printer_test_failed_duration else R.string.printer_test_duration
        appendLine(resources.getString(format, resources.getString(stage.label()), elapsed))
    }
    append(resources.getString(R.string.printer_test_duration, resources.getString(R.string.printer_test_total), totalMs))
}

@StringRes
private fun PrinterTestReport.status(): Int = when (error) {
    null -> R.string.printer_test_sent
    PrinterTestError.PERMISSION -> R.string.printer_test_permission
    PrinterTestError.PRINTER_NOT_FOUND -> R.string.printer_test_missing
    PrinterTestError.CONNECTION -> R.string.printer_test_connection_error
    PrinterTestError.TIMEOUT -> R.string.printer_test_timeout_error
    PrinterTestError.OTHER -> R.string.printer_test_error
}

@StringRes
private fun PrinterTestStage.label(): Int = when (this) {
    PrinterTestStage.QUEUE -> R.string.printer_test_queue
    PrinterTestStage.PREPARE -> R.string.printer_test_prepare
    PrinterTestStage.CONNECT -> R.string.printer_test_connect
    PrinterTestStage.SEND -> R.string.printer_test_send
}
