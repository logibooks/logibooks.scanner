// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.scan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * HID scan collector state machine for Bluetooth HID "keyboard wedge" scanners.
 * 
 * This class buffers printable characters and applies heuristics to distinguish
 * scanner input from human typing:
 * - Speed gate: median inter-key interval must be scanner-fast (< 35ms default)
 * - Minimum length: at least 6 characters by default
 * - Max duration guard: total burst < 500ms
 * - Debounce: suppress identical scans within 300ms
 * 
 * A scan is finalized when:
 * 1. A terminator key is received (Enter or Tab)
 * 2. No new character arrives for idleTimeoutMs (default 70ms)
 * 
 * Pure Kotlin implementation with no Android dependencies for testability.
 */
class HidScanCollector(
    private val minLength: Int = 6,
    private val idleTimeoutMs: Long = 70,
    private val maxScanDurationMs: Long = 500,
    maxMedianInterKeyMs: Long = 35,
    private val debounceMs: Long = 300,
    private val scope: CoroutineScope,
    private val onScan: (String) -> Unit,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val maxMedianInterKeyMsDouble = maxMedianInterKeyMs.toDouble()
    private val buffer = StringBuilder()
    private val timestamps = mutableListOf<Long>()
    private var idleJob: Job? = null
    private var lastScanValue: String? = null
    private var lastScanTime: Long = 0
    private var firstCharTime: Long = 0

    /**
     * Called when a printable character is received from HID input.
     */
    fun onPrintableChar(c: Char) {
        val now = currentTimeMillis()
        
        // Start new scan burst if buffer is empty
        if (buffer.isEmpty()) {
            firstCharTime = now
        }
        
        buffer.append(c)
        timestamps.add(now)
        
        // Reset idle timeout
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(idleTimeoutMs)
            finalizeIfValid()
        }
    }

    /**
     * Called when a terminator key is received (Enter or Tab).
     */
    fun onTerminator() {
        idleJob?.cancel()
        finalizeIfValid()
    }

    /**
     * Reset the collector state.
     */
    fun reset() {
        idleJob?.cancel()
        buffer.clear()
        timestamps.clear()
        firstCharTime = 0
    }

    private fun finalizeIfValid() {
        if (buffer.isEmpty()) {
            reset()
            return
        }

        val candidate = buffer.toString()
            .trim()
            .trimEnd('\r', '\n', '\t')
        
        if (candidate.isEmpty()) {
            reset()
            return
        }

        // Apply gates
        if (!passesGates(candidate)) {
            reset()
            return
        }

        // Apply debounce
        val now = currentTimeMillis()
        if (candidate == lastScanValue && (now - lastScanTime) < debounceMs) {
            reset()
            return
        }

        // Valid scan - emit
        lastScanValue = candidate
        lastScanTime = now
        reset()
        onScan(candidate)
    }

    private fun passesGates(candidate: String): Boolean {
        // Minimum length gate
        if (candidate.length < minLength) {
            return false
        }

        // Need at least 2 timestamps to compute intervals
        if (timestamps.size < 2) {
            return false
        }

        // Max duration guard
        val totalDuration = timestamps.last() - firstCharTime
        if (totalDuration > maxScanDurationMs) {
            return false
        }

        // Speed gate: median inter-key interval
        val intervals = mutableListOf<Long>()
        for (i in 1 until timestamps.size) {
            intervals.add(timestamps[i] - timestamps[i - 1])
        }
        intervals.sort()
        val median = if (intervals.size % 2 == 0) {
            val upperIndex = intervals.size / 2
            val lowerIndex = upperIndex - 1
            (intervals[lowerIndex] + intervals[upperIndex]).toDouble() / 2.0
        } else {
            intervals[intervals.size / 2].toDouble()
        }

        if (median > maxMedianInterKeyMsDouble) {
            return false
        }

        return true
    }
}
