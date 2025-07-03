/* 
 * FLAC library (Java)
 * 
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/flac-library-java
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (see COPYING.txt and COPYING.LESSER.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */
package io.nayuki.flac.decode

import korlibs.io.lang.*

/**
 * A basic implementation of most functionality required by FlacLowLevelInpuut.
 */
abstract class AbstractFlacLowLevelInput : FlacLowLevelInput {
    /*---- Fields ----*/ // Data from the underlying stream is first stored into this byte buffer before further processing.
    private var byteBufferStartPos: Long = 0
    private var byteBuffer: ByteArray?
    private var byteBufferLen = 0
    private var byteBufferIndex = 0

    // The buffer of next bits to return to a reader. Note that byteBufferIndex is incremented when byte
    // values are put into the bit buffer, but they might not have been consumed by the ultimate reader yet.
    private var bitBuffer: Long = 0 // Only the bottom bitBufferLen bits are valid; the top bits are garbage.
    private var bitBufferLen = 0 // Always in the range [0, 64].

    // Current state of the CRC calculations.
    private var crc8 = 0 // Always a uint8 value.
    private var crc16 = 0 // Always a uint16 value.
    private var crcStartIndex = 0 // In the range [0, byteBufferLen], unless byteBufferLen = -1.

    /*---- Methods ----*/ /*-- Stream position --*/
    override val position: Long
        get() = byteBufferStartPos + byteBufferIndex - (bitBufferLen + 7) / 8

    override val bitPosition: Int
        get() = -bitBufferLen and 7

    // When a subclass handles seekTo() and didn't throw UnsupportedOperationException,
    // it must call this method to flush the buffers of upcoming data.
    protected fun positionChanged(pos: Long) {
        byteBufferStartPos = pos
        byteBuffer?.fill(0) // Defensive clearing, should have no visible effect outside of debugging
        byteBufferLen = 0
        byteBufferIndex = 0
        bitBuffer = 0 // Defensive clearing, should have no visible effect outside of debugging
        bitBufferLen = 0
        resetCrcs()
    }

    // Either returns silently or throws an exception.
    private fun checkByteAligned() {
        check(bitBufferLen % 8 == 0) { "Not at a byte boundary" }
    }

    /*-- Reading bitwise integers --*/
    @Throws(IOException::class)
    override fun readUint(n: Int): Int {
        require(!(n < 0 || n > 32))
        while (bitBufferLen < n) {
            val b = readUnderlying()
            if (b == -1) throw EOFException("EOF")
            bitBuffer = bitBuffer shl 8 or b.toLong()
            bitBufferLen += 8
            assert(0 <= bitBufferLen && bitBufferLen <= 64)
        }
        var result = (bitBuffer ushr bitBufferLen - n).toInt()
        if (n != 32) {
            result = result and (1 shl n) - 1
            assert(result ushr n == 0)
        }
        bitBufferLen -= n
        assert(0 <= bitBufferLen && bitBufferLen <= 64)
        return result
    }

    @Throws(IOException::class)
    override fun readSignedInt(n: Int): Int {
        val shift = 32 - n
        return readUint(n) shl shift shr shift
    }

    @Throws(IOException::class)
    override fun readRiceSignedInts(param: Int, result: LongArray, start: Int, end: Int) {
        var start = start
        require(!(param < 0 || param > 31))
        val unaryLimit = 1L shl 53 - param
        val consumeTable = RICE_DECODING_CONSUMED_TABLES[param]
        val valueTable = RICE_DECODING_VALUE_TABLES[param]
        while (true) {
            middle@ while (start <= end - RICE_DECODING_CHUNK) {
                if (bitBufferLen < RICE_DECODING_CHUNK * RICE_DECODING_TABLE_BITS) {
                    if (byteBufferIndex <= byteBufferLen - 8) {
                        fillBitBuffer()
                    } else break
                }
                var i = 0
                while (i < RICE_DECODING_CHUNK) {

                    // Fast decoder
                    val extractedBits =
                        (bitBuffer ushr bitBufferLen - RICE_DECODING_TABLE_BITS).toInt() and RICE_DECODING_TABLE_MASK
                    val consumed = consumeTable[extractedBits].toInt()
                    if (consumed == 0) break@middle
                    bitBufferLen -= consumed
                    result[start] = valueTable[extractedBits].toLong()
                    i++
                    start++
                }
            }

            // Slow decoder
            if (start >= end) break
            var `val`: Long = 0
            while (readUint(1) == 0) {
                if (`val` >= unaryLimit) {
                    // At this point, the final decoded value would be so large that the result of the
                    // downstream restoreLpc() calculation would not fit in the output sample's bit depth -
                    // hence why we stop early and throw an exception. However, this check is conservative
                    // and doesn't catch all the cases where the post-LPC result wouldn't fit.
                    throw DataFormatException("Residual value too large")
                }
                `val`++
            }
            `val` = `val` shl param or readUint(param).toLong() // Note: Long masking unnecessary because param <= 31
            assert(
                `val` ushr 53 == 0L // Must fit a uint53 by design due to unaryLimit
            )
            `val` =
                `val` ushr 1 xor -(`val` and 1L) // Transform uint53 to int53 according to Rice coding of signed numbers
            assert(
                `val` shr 52 == 0L || `val` shr 52 == -1L // Must fit a signed int53 by design
            )
            result[start] = `val`
            start++
        }
    }

    // Appends at least 8 bits to the bit buffer, or throws EOFException.
    @Throws(IOException::class)
    private fun fillBitBuffer() {
        var i = byteBufferIndex
        val n = kotlin.math.min(64 - bitBufferLen ushr 3, byteBufferLen - i)
        val b = byteBuffer
        if (n > 0) {
            var j = 0
            while (j < n) {
                bitBuffer = bitBuffer shl 8 or (b!![i].toInt() and 0xFF).toLong()
                j++
                i++
            }
            bitBufferLen += n shl 3
        } else if (bitBufferLen <= 56) {
            val temp = readUnderlying()
            if (temp == -1) throw EOFException("EOF")
            bitBuffer = bitBuffer shl 8 or temp.toLong()
            bitBufferLen += 8
        }
        assert(8 <= bitBufferLen && bitBufferLen <= 64)
        byteBufferIndex += n
    }

    /*-- Reading bytes --*/
    @Throws(IOException::class)
    override fun readByte(): Int {
        checkByteAligned()
        return if (bitBufferLen >= 8) readUint(8) else {
            assert(bitBufferLen == 0)
            readUnderlying()
        }
    }

    @Throws(IOException::class)
    override fun readFully(b: ByteArray) {
        checkByteAligned()
        for (i in b.indices) b[i] = readUint(8).toByte()
    }

    // Reads a byte from the byte buffer (if available) or from the underlying stream, returning either a uint8 or -1.
    @Throws(IOException::class)
    private fun readUnderlying(): Int {
        if (byteBufferIndex >= byteBufferLen) {
            if (byteBufferLen == -1) return -1
            byteBufferStartPos += byteBufferLen.toLong()
            updateCrcs(0)
            byteBufferLen = readUnderlying(byteBuffer!!, 0, byteBuffer!!.size)
            crcStartIndex = 0
            if (byteBufferLen <= 0) return -1
            byteBufferIndex = 0
        }
        assert(byteBufferIndex < byteBufferLen)
        val temp = byteBuffer!![byteBufferIndex].toInt() and 0xFF
        byteBufferIndex++
        return temp
    }

    // Reads up to 'len' bytes from the underlying byte-based input stream into the given array subrange.
    // Returns a value in the range [0, len] for a successful read, or -1 if the end of stream was reached.
    @Throws(IOException::class)
    protected abstract fun readUnderlying(buf: ByteArray, off: Int, len: Int): Int

    /*-- CRC calculations --*/
    override fun resetCrcs() {
        checkByteAligned()
        crcStartIndex = byteBufferIndex - bitBufferLen / 8
        crc8 = 0
        crc16 = 0
    }

    override fun getCrc8(): Int {
        checkByteAligned()
        updateCrcs(bitBufferLen / 8)
        if (crc8 ushr 8 != 0) throw AssertionError()
        return crc8
    }

    override fun getCrc16(): Int {
        checkByteAligned()
        updateCrcs(bitBufferLen / 8)
        if (crc16 ushr 16 != 0) throw AssertionError()
        return crc16
    }

    // Updates the two CRC values with data in byteBuffer[crcStartIndex : byteBufferIndex - unusedTrailingBytes].
    private fun updateCrcs(unusedTrailingBytes: Int) {
        val end = byteBufferIndex - unusedTrailingBytes
        for (i in crcStartIndex until end) {
            val b = byteBuffer!![i].toInt() and 0xFF
            crc8 = CRC8_TABLE[crc8 xor b].toInt() and 0xFF
            crc16 = CRC16_TABLE[crc16 ushr 8 xor b].code xor (crc16 and 0xFF shl 8)
            assert(crc8 ushr 8 == 0)
            assert(crc16 ushr 16 == 0)
        }
        crcStartIndex = end
    }

    /*-- Miscellaneous --*/ // Note: This class only uses memory and has no native resources. It's not strictly necessary to
    // call the implementation of AbstractFlacLowLevelInput.close() here, but it's a good habit anyway.
    override fun close() {
        byteBuffer = null
        byteBufferLen = -1
        byteBufferIndex = -1
        bitBuffer = 0
        bitBufferLen = -1
        crc8 = -1
        crc16 = -1
        crcStartIndex = -1
    }

    /*---- Constructors ----*/
    init {
        byteBuffer = ByteArray(4096)
        positionChanged(0)
    }

    companion object {
        /*---- Tables of constants ----*/ // For Rice decoding
        private const val RICE_DECODING_TABLE_BITS = 13 // Configurable, must be positive
        private const val RICE_DECODING_TABLE_MASK = (1 shl RICE_DECODING_TABLE_BITS) - 1
        private val RICE_DECODING_CONSUMED_TABLES = Array(31) { ByteArray(1 shl RICE_DECODING_TABLE_BITS) }
        private val RICE_DECODING_VALUE_TABLES = Array(31) { IntArray(1 shl RICE_DECODING_TABLE_BITS) }
        private const val RICE_DECODING_CHUNK =
            4 // Configurable, must be positive, and RICE_DECODING_CHUNK * RICE_DECODING_TABLE_BITS <= 64

        init {
            for (param in RICE_DECODING_CONSUMED_TABLES.indices) {
                val consumed = RICE_DECODING_CONSUMED_TABLES[param]
                val values = RICE_DECODING_VALUE_TABLES[param]
                var i = 0
                while (true) {
                    val numBits = (i ushr param) + 1 + param
                    if (numBits > RICE_DECODING_TABLE_BITS) break
                    val bits = 1 shl param or (i and (1 shl param) - 1)
                    val shift = RICE_DECODING_TABLE_BITS - numBits
                    for (j in 0 until (1 shl shift)) {
                        consumed[bits shl shift or j] = numBits.toByte()
                        values[bits shl shift or j] = i ushr 1 xor -(i and 1)
                    }
                    i++
                }
                if (consumed[0].toInt() != 0) throw AssertionError()
            }
        }

        // For CRC calculations
        private val CRC8_TABLE = ByteArray(256)
        private val CRC16_TABLE = CharArray(256)

        init {
            for (i in CRC8_TABLE.indices) {
                var temp8 = i
                var temp16 = i shl 8
                for (j in 0..7) {
                    temp8 = temp8 shl 1 xor (temp8 ushr 7) * 0x107
                    temp16 = temp16 shl 1 xor (temp16 ushr 15) * 0x18005
                }
                CRC8_TABLE[i] = temp8.toByte()
                CRC16_TABLE[i] = temp16.toChar()
            }
        }
    }
}
