// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.printer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException

internal data class AutoPrintPrinter(val jobId: Int, val address: String)

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
    // Keep connection and write ordering separate from socket ownership. Changing jobs can close
    // an unused socket immediately, even while its connect() is blocking on the I/O dispatcher.
    private val operations = Mutex()
    private val ownership = Any()
    private var active: Session? = null

    private class Session(val printer: AutoPrintPrinter) {
        lateinit var warmup: Deferred<Result<Unit>>
        var socket: PrinterConnection? = null
        var printing = false
    }

    fun select(printer: AutoPrintPrinter?) {
        val warmup = synchronized(ownership) {
            val previous = active
            if (previous?.printer == printer) return
            if (previous != null && !previous.printing) release(previous)
            active = printer?.let(::Session)
            active?.let { session ->
                scope.async(io, start = CoroutineStart.LAZY) {
                    operations.withLock {
                        // A failed preconnect is reported only if a print was already waiting for it.
                        runCatching {
                            if (synchronized(ownership) { active === session }) {
                                connect(session.printer.address, session)
                            }
                            Unit
                        }
                    }
                }.also { session.warmup = it }
            }
        }
        warmup?.start()
    }

    fun preparePrint(address: String): suspend (ByteArray) -> Unit {
        val session = synchronized(ownership) { active?.takeIf { it.printer.address == address } }
        return { payload ->
            val pendingWarmup = session?.warmup?.takeUnless { it.isCompleted }
            withContext(io) {
                val warmup = pendingWarmup?.await()
                operations.withLock {
                    val (owner, connected) = synchronized(ownership) {
                        // Old work can use an already open socket for the same printer, but only
                        // a request from the current session can open a socket for reuse.
                        val owner = active?.takeIf {
                            it === session || (it.printer.address == address && it.socket != null)
                        }
                        owner?.printing = true
                        owner to owner?.socket
                    }
                    try {
                        if (owner === session) warmup?.getOrThrow()
                        val socket = connected ?: checkNotNull(connect(address, owner))
                        try {
                            socket.write(payload)
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
            }
        }
    }

    private suspend fun connect(address: String, owner: Session?): PrinterConnection? {
        val socket = openSocket(address)
        if (owner != null) synchronized(ownership) {
            if (owner !== active && !owner.printing) {
                close(socket)
                return null
            }
            owner.socket = socket
        }
        try {
            socket.connect()
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
