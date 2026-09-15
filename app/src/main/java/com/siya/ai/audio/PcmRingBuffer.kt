package com.siya.ai.audio

/**
 * Small thread-safe PCM16 ring buffer. Overflow drops the oldest samples so the
 * live voice pipeline never grows without bound.
 */
class PcmRingBuffer(private val capacity: Int) {
    init { require(capacity > 0) }

    private val data = ShortArray(capacity)
    private var readIndex = 0
    private var writeIndex = 0
    private var size = 0

    @Synchronized
    fun write(source: ShortArray, offset: Int = 0, length: Int = source.size): Int {
        require(offset >= 0 && length >= 0 && offset + length <= source.size)
        var written = 0
        for (i in 0 until length) {
            if (size == capacity) {
                readIndex = (readIndex + 1) % capacity
                size--
            }
            data[writeIndex] = source[offset + i]
            writeIndex = (writeIndex + 1) % capacity
            size++
            written++
        }
        return written
    }

    @Synchronized
    fun read(destination: ShortArray, offset: Int = 0, length: Int = destination.size): Int {
        require(offset >= 0 && length >= 0 && offset + length <= destination.size)
        val count = minOf(length, size)
        repeat(count) {
            destination[offset + it] = data[readIndex]
            readIndex = (readIndex + 1) % capacity
        }
        size -= count
        return count
    }

    @Synchronized fun available(): Int = size
    @Synchronized fun clear() { readIndex = 0; writeIndex = 0; size = 0 }
}
