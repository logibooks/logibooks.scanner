// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

internal class ScanInputQueue<T : Any> {
    private val queued = ArrayDeque<T>()
    private var active = false

    fun begin(scan: T): Boolean {
        if (active) {
            queued.addLast(scan)
            return false
        }
        active = true
        return true
    }

    fun finish(): T? = queued.removeFirstOrNull().also { if (it == null) active = false }

    fun discardQueued(): List<T> = queued.toList().also { queued.clear() }

    fun reset() {
        queued.clear()
        active = false
    }
}
