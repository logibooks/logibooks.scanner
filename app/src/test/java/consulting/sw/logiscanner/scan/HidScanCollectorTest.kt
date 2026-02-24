// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.scan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HidScanCollector
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HidScanCollectorTest {

    private lateinit var testScope: CoroutineScope
    private val capturedScans = mutableListOf<String>()
    private var virtualTime = 0L

    @Before
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = CoroutineScope(testDispatcher)
        capturedScans.clear()
        virtualTime = 0L
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun fastBurstWithTerminator_emitsScan() = runTest {
        val collector = HidScanCollector(
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Simulate fast scanner input (< 35ms between chars)
        "ABC12345".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10) // 10ms between chars
        }

        // Send terminator
        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(1, capturedScans.size)
        assertEquals("ABC12345", capturedScans[0])
    }

    @Test
    fun fastBurstWithoutTerminator_emitsAfterIdleTimeout() = runTest {
        val collector = HidScanCollector(
            idleTimeoutMs = 70,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Simulate fast scanner input without terminator
        "XYZ789012".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 15
            advanceTimeBy(15) // 15ms between chars
        }

        // Wait for idle timeout (70ms)
        virtualTime += 80
        advanceTimeBy(80)
        advanceUntilIdle()

        assertEquals(1, capturedScans.size)
        assertEquals("XYZ789012", capturedScans[0])
    }

    @Test
    fun slowTyping_doesNotEmitScan() = runTest {
        val collector = HidScanCollector(
            maxMedianInterKeyMs = 35,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Simulate slow human typing (> 35ms median)
        "ABCDEF123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 100
            advanceTimeBy(100) // 100ms between chars (too slow)
        }

        collector.onTerminator()
        advanceUntilIdle()

        // Should not emit because it's too slow
        assertEquals(0, capturedScans.size)
    }

    @Test
    fun tooShortInput_doesNotEmit() = runTest {
        val collector = HidScanCollector(
            minLength = 6,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Only 5 characters (below minimum of 6)
        "ABC12".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }

        collector.onTerminator()
        advanceUntilIdle()

        // Should not emit because it's too short
        assertEquals(0, capturedScans.size)
    }

    @Test
    fun duplicateWithinDebounce_suppressed() = runTest {
        val collector = HidScanCollector(
            debounceMs = 300,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // First scan
        "BARCODE123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()

        // Immediately scan the same code again (within 300ms)
        virtualTime += 100
        advanceTimeBy(100) // Only 100ms passed
        "BARCODE123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()

        // Should only have first scan, second is debounced
        assertEquals(1, capturedScans.size)
        assertEquals("BARCODE123", capturedScans[0])
    }

    @Test
    fun duplicateAfterDebounce_notSuppressed() = runTest {
        val collector = HidScanCollector(
            debounceMs = 300,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // First scan
        "BARCODE123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()

        // Wait for debounce period to pass
        virtualTime += 350
        advanceTimeBy(350)

        // Scan the same code again (after 300ms debounce)
        "BARCODE123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()

        // Should have both scans
        assertEquals(2, capturedScans.size)
        assertEquals("BARCODE123", capturedScans[0])
        assertEquals("BARCODE123", capturedScans[1])
    }

    @Test
    fun maxDurationExceeded_doesNotEmit() = runTest {
        val collector = HidScanCollector(
            maxScanDurationMs = 500,
            maxMedianInterKeyMs = 100, // Allow slower typing for this test
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Simulate input that takes > 500ms total
        // 11 chars with 50ms intervals = 10 intervals * 50ms = 500ms, so we need more
        "LONGCODE123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 51
            advanceTimeBy(51) // Total: 51 * 10 intervals = 510ms (> 500ms)
        }

        collector.onTerminator()
        advanceUntilIdle()

        // Should not emit because total duration exceeded 500ms
        assertEquals(0, capturedScans.size)
    }

    @Test
    fun trailingWhitespace_trimmed() = runTest {
        val collector = HidScanCollector(
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        "BARCODE  ".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }

        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(1, capturedScans.size)
        assertEquals("BARCODE", capturedScans[0])
    }

    @Test
    fun leadingWhitespace_trimmed() = runTest {
        val collector = HidScanCollector(
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        "  BARCODE".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }

        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(1, capturedScans.size)
        assertEquals("BARCODE", capturedScans[0])
    }

    @Test
    fun emptyAfterTrim_doesNotEmit() = runTest {
        val collector = HidScanCollector(
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        "     ".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }

        collector.onTerminator()
        advanceUntilIdle()

        // Should not emit empty string
        assertEquals(0, capturedScans.size)
    }

    @Test
    fun reset_clearsBuffer() = runTest {
        val collector = HidScanCollector(
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        "ABC123".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }

        // Reset before terminator
        collector.reset()
        advanceUntilIdle()

        // Should not emit
        assertEquals(0, capturedScans.size)

        // New scan should work
        "XYZ789ABC".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(1, capturedScans.size)
        assertEquals("XYZ789ABC", capturedScans[0])
    }

    @Test
    fun multipleScans_allEmitted() = runTest {
        val collector = HidScanCollector(
            debounceMs = 100,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // First scan
        "SCAN001".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()
        virtualTime += 150
        advanceTimeBy(150)

        // Second scan
        "SCAN002".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()
        virtualTime += 150
        advanceTimeBy(150)

        // Third scan
        "SCAN003".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 10
            advanceTimeBy(10)
        }
        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(3, capturedScans.size)
        assertEquals("SCAN001", capturedScans[0])
        assertEquals("SCAN002", capturedScans[1])
        assertEquals("SCAN003", capturedScans[2])
    }

    @Test
    fun medianCalculation_withEvenNumberOfIntervals() = runTest {
        val collector = HidScanCollector(
            maxMedianInterKeyMs = 30,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Create a pattern with even number of intervals
        // Intervals: 10, 20, 25, 35 ms
        // Sorted: 10, 20, 25, 35
        // Median of 4 values = (20 + 25) / 2 = 22.5 < 30 ✓
        collector.onPrintableChar('A')
        virtualTime += 10
        advanceTimeBy(10)
        collector.onPrintableChar('B')
        virtualTime += 20
        advanceTimeBy(20)
        collector.onPrintableChar('C')
        virtualTime += 25
        advanceTimeBy(25)
        collector.onPrintableChar('D')
        virtualTime += 35
        advanceTimeBy(35)
        collector.onPrintableChar('E')

        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(0, capturedScans.size) // Too short (< 6 chars)
    }

    @Test
    fun medianCalculation_withOddNumberOfIntervals() = runTest {
        val collector = HidScanCollector(
            maxMedianInterKeyMs = 25,
            scope = testScope,
            onScan = { capturedScans.add(it) },
            currentTimeMillis = { virtualTime }
        )

        // Create a pattern with odd number of intervals (7 chars = 6 intervals)
        // Intervals: 10, 15, 20, 25, 30 ms repeated
        // Sorted middle value should be around 20ms < 25 ✓
        "ABCDEFG".forEach { c ->
            collector.onPrintableChar(c)
            virtualTime += 20
            advanceTimeBy(20)
        }

        collector.onTerminator()
        advanceUntilIdle()

        assertEquals(1, capturedScans.size)
        assertEquals("ABCDEFG", capturedScans[0])
    }
}
