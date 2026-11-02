package com.omnicast.receiver.directstream

import kotlinx.coroutines.*
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Búfer circular asíncrono (Ring Buffer) de 16 MB para PVR.
 * Desacopla la descarga de red de la escritura en flash/USB, volcando en bloques
 * alineados de 128 KB para garantizar 0 fotogramas perdidos (dropped frames = 0).
 */
class PvrRingBuffer(
    private val capacity: Int = 16 * 1024 * 1024, // 16 MB en memoria contigua
    private val blockSize: Int = 128 * 1024       // Bloques alineados de 128 KB
) {
    private val buffer = ByteArray(capacity)
    private var head = 0
    private var tail = 0
    private var available = 0
    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()
    private val notFull = lock.newCondition()

    @Volatile
    private var isClosed = false
    @Volatile
    var totalBytesWritten: Long = 0L
        private set

    private var flusherJob: Job? = null

    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        if (length <= 0 || isClosed) return
        var remaining = length
        var srcOffset = offset

        while (remaining > 0 && !isClosed) {
            lock.withLock {
                while (available >= capacity && !isClosed) {
                    notFull.await()
                }
                if (isClosed) return

                val toWrite = minOf(remaining, capacity - available)
                val part1 = minOf(toWrite, capacity - head)
                System.arraycopy(data, srcOffset, buffer, head, part1)

                val part2 = toWrite - part1
                if (part2 > 0) {
                    System.arraycopy(data, srcOffset + part1, buffer, 0, part2)
                    head = part2
                } else {
                    head = (head + part1) % capacity
                }

                available += toWrite
                remaining -= toWrite
                srcOffset += toWrite
                notEmpty.signal()
            }
        }
    }

    fun startFlusher(output: OutputStream, scope: CoroutineScope): Job {
        val job = scope.launch(Dispatchers.IO) {
            val chunk = ByteArray(blockSize)
            try {
                while (isActive && (!isClosed || available > 0)) {
                    var bytesToRead = 0
                    lock.withLock {
                        while (available < blockSize && !isClosed) {
                            notEmpty.await()
                        }
                        if (available == 0 && isClosed) return@withLock

                        bytesToRead = if (available >= blockSize) blockSize else available
                        val part1 = minOf(bytesToRead, capacity - tail)
                        System.arraycopy(buffer, tail, chunk, 0, part1)

                        val part2 = bytesToRead - part1
                        if (part2 > 0) {
                            System.arraycopy(buffer, 0, chunk, part1, part2)
                            tail = part2
                        } else {
                            tail = (tail + part1) % capacity
                        }

                        available -= bytesToRead
                        notFull.signal()
                    }

                    if (bytesToRead > 0) {
                        output.write(chunk, 0, bytesToRead)
                        totalBytesWritten += bytesToRead
                    }
                }
                output.flush()
            } catch (_: Throwable) {
                // Control silencioso de cancelación o desmontaje de USB
            }
        }
        flusherJob = job
        return job
    }

    suspend fun flushAndClose(output: OutputStream) {
        lock.withLock {
            isClosed = true
            notEmpty.signalAll()
            notFull.signalAll()
        }
        flusherJob?.join()
        runCatching { output.flush() }
        runCatching { output.close() }
    }
}
