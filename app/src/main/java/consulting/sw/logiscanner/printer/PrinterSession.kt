// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal interface PrinterConnection : Closeable {
    suspend fun connect()
    suspend fun write(payload: ByteArray)
    suspend fun awaitDisconnect()
}

internal class PrinterSession(
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val openSocket: (String) -> PrinterConnection
) {
    // Keep connection and write ordering separate from socket ownership. Changing printers can close
    // an unused socket immediately, even while its connect() is blocking on the I/O dispatcher.
    private val operations = Mutex()
    private val ownership = Any()
    private var active: Session? = null

    private class Session(val address: String) {
        lateinit var warmup: Deferred<Result<Unit>>
        var socket: PrinterConnection? = null
        var printing = false
    }

    fun select(address: String?) {
        val warmup = synchronized(ownership) {
            val previous = active
            if (previous?.address == address) return
            if (previous != null && !previous.printing) release(previous)
            active = address?.let(::Session)
            active?.let(::createWarmup)
        }
        warmup?.start()
    }

    fun warmUp(address: String) {
        val warmup = synchronized(ownership) {
            val session = active ?: return
            if (session.address != address || session.socket != null || !session.warmup.isCompleted) return
            createWarmup(session)
        }
        warmup.start()
    }

    private fun createWarmup(session: Session): Deferred<Result<Unit>> =
        scope.async(io, start = CoroutineStart.LAZY) {
            operations.withLock {
                // A failed preconnect is reported only if a print was already waiting for it.
                runCatching {
                    if (synchronized(ownership) { active === session && session.socket == null }) {
                        connect(session.address, session, timed = true)
                    }
                    Unit
                }
            }
        }.also { session.warmup = it }

    fun preparePrint(address: String): suspend (ByteArray) -> Unit {
        val session = sessionFor(address)
        return { payload ->
            val pendingWarmup = pendingWarmup(session)
            withContext(io) {
                val warmup = pendingWarmup?.await()
                operations.withLock { sendLocked(address, payload, session, warmup) }
            }
        }
    }

    suspend fun printTest(address: String, payload: ByteArray, timing: PrinterTestTiming) {
        val session = sessionFor(address)
        val pendingWarmup = pendingWarmup(session)
        withContext(io) {
            val warmup = pendingWarmup?.let {
                timing.measure(PrinterTestStage.CONNECT) {
                    withTimeoutOrNull(15_000) { it.await() } ?: throw PrinterTestFailure(PrinterTestError.TIMEOUT)
                }
            }
            timing.withQueueLock(operations) { sendLocked(address, payload, session, warmup, timing) }
        }
    }

    private fun sessionFor(address: String): Session? = synchronized(ownership) {
        active?.takeIf { it.address == address }
    }

    private fun pendingWarmup(session: Session?): Deferred<Result<Unit>>? = synchronized(ownership) {
        session?.warmup?.takeUnless { it.isCompleted }
    }

    private suspend fun sendLocked(
        address: String,
        payload: ByteArray,
        session: Session?,
        warmup: Result<Unit>?,
        timing: PrinterTestTiming? = null
    ) {
        val (owner, connected) = synchronized(ownership) {
            // A stale request may use an open socket for its address. Only current work may retain a new socket.
            val owner = active?.takeIf { it === session || (it.address == address && it.socket != null) }
            owner?.printing = true
            owner to owner?.socket
        }
        try {
            suspend fun connection(): PrinterConnection {
                if (owner === session) warmup?.getOrThrow()
                return connected ?: checkNotNull(connect(address, owner, timed = timing != null))
            }
            val socket = if (timing == null) connection() else timing.measure(PrinterTestStage.CONNECT) { connection() }
            try {
                if (timing == null) {
                    socket.write(payload)
                } else {
                    timing.measure(PrinterTestStage.SEND) { socket.runTestIo(5_000) { write(payload) } }
                }
            } catch (failure: Exception) {
                if (owner != null) discard(owner, socket)
                throw failure
            } finally {
                if (owner == null) close(socket)
            }
        } finally {
            if (owner != null) synchronized(ownership) {
                owner.printing = false
                if (active !== owner) release(owner)
            }
        }
    }

    private suspend fun PrinterConnection.runTestIo(
        timeoutMs: Long,
        operation: suspend PrinterConnection.() -> Unit
    ) {
        val socket = this
        withTimeoutOrNull(timeoutMs) {
            coroutineScope {
                suspendCancellableCoroutine<Unit> { continuation ->
                    // Close independently of the worker so cancellation unblocks RFCOMM I/O.
                    continuation.invokeOnCancellation { close(socket) }
                    launch(io) {
                        if (!continuation.isActive) return@launch
                        try {
                            socket.operation()
                            continuation.resume(Unit)
                        } catch (failure: Exception) {
                            continuation.resumeWithException(failure)
                        }
                    }
                }
            }
        } ?: throw PrinterTestFailure(PrinterTestError.TIMEOUT)
    }

    private suspend fun connect(address: String, owner: Session?, timed: Boolean): PrinterConnection? {
        val socket = openSocket(address)
        if (owner != null) synchronized(ownership) {
            if (owner !== active && !owner.printing) {
                close(socket)
                return null
            }
            owner.socket = socket
        }
        try {
            if (timed) socket.runTestIo(15_000) { connect() } else socket.connect()
        } catch (failure: Exception) {
            discard(owner, socket)
            throw failure
        }
        if (owner != null) scope.launch(io) {
            try {
                socket.awaitDisconnect()
            } catch (_: IOException) {
                // Android reports a closed RFCOMM stream as EOF or an IOException.
            } finally {
                discard(owner, socket)
            }
        }
        return socket
    }

    private fun discard(owner: Session?, socket: PrinterConnection) = synchronized(ownership) {
        if (owner == null) close(socket)
        else if (owner.socket === socket) release(owner)
    }

    private fun release(owner: Session) {
        val socket = owner.socket
        owner.socket = null
        socket?.let(::close)
    }

    private fun close(socket: PrinterConnection) {
        try {
            socket.close()
        } catch (_: IOException) {
            // The socket is no longer used, even if Android reports a close failure.
        }
    }
}
