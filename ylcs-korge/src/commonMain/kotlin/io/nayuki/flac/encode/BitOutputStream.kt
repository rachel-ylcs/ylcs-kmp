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
package io.nayuki.flac.encode

/*
import java.io.IOException
import java.io.OutputStream
import java.util.*

/* 
 * A bit-oriented output stream, with other methods tailored for FLAC usage (such as CRC calculation).
 */
class BitOutputStream(out: OutputStream) : AutoCloseable {
    /*---- Fields ----*/
    private var out // The underlying byte-based output stream to write to.
            : OutputStream?
    private var bitBuffer // Only the bottom bitBufferLen bits are valid; the top bits are garbage.
            : Long = 0
    private var bitBufferLen // Always in the range [0, 64].
            : Int = 0
    private var byteCount // Number of bytes written since the start of stream.
            : Long = 0

    // Current state of the CRC calculations.
    internal var crc8: Int = 0 // Always a uint8 value.
    private var crc16: Int = 0 // Always a uint16 value.

    /*---- Constructors ----*/ // Constructs a FLAC-oriented bit output stream from the given byte-based output stream.
    init {
        this.out = Objects.requireNonNull(out)
        resetCrcs()
    }

    /*---- Methods ----*/ /*-- Bit position --*/ // Writes between 0 to 7 zero bits, to align the current bit position to a byte boundary.
    @Throws(IOException::class)
    fun alignToByte() {
        writeInt((64 - bitBufferLen) % 8, 0)
    }

    // Either returns silently or throws an exception.
    private fun checkByteAligned() {
        if (bitBufferLen % 8 != 0) throw IllegalStateException("Not at a byte boundary")
    }

    /*-- Writing bitwise integers --*/ // Writes the lowest n bits of the given value to this bit output stream.
    // This doesn't care whether val represents a signed or unsigned integer.
    @Throws(IOException::class)
    fun writeInt(n: Int, `val`: Int) {
        if (n < 0 || n > 32) throw IllegalArgumentException()
        if (bitBufferLen + n > 64) {
            flush()
            assert(bitBufferLen + n <= 64)
        }
        bitBuffer = bitBuffer shl n
        bitBuffer = bitBuffer or (`val`.toLong() and ((1L shl n) - 1))
        bitBufferLen += n
        assert(0 <= bitBufferLen && bitBufferLen <= 64)
    }

    // Writes out whole bytes from the bit buffer to the underlying stream. After this is done,
    // only 0 to 7 bits remain in the bit buffer. Also updates the CRCs on each byte written.
    @Throws(IOException::class)
    fun flush() {
        while (bitBufferLen >= 8) {
            bitBufferLen -= 8
            val b: Int = (bitBuffer ushr bitBufferLen).toInt() and 0xFF
            out!!.write(b)
            byteCount++
            crc8 = crc8 xor b
            crc16 = crc16 xor (b shl 8)
            for (i in 0..7) {
                crc8 = crc8 shl 1
                crc16 = crc16 shl 1
                crc8 = crc8 xor ((crc8 ushr 8) * 0x107)
                crc16 = crc16 xor ((crc16 ushr 16) * 0x18005)
                assert((crc8 ushr 8) == 0)
                assert((crc16 ushr 16) == 0)
            }
        }
        assert(0 <= bitBufferLen && bitBufferLen <= 64)
        out!!.flush()
    }

    /*-- CRC calculations --*/ // Marks the current position (which must be byte-aligned) as the start of both CRC calculations.
    @Throws(IOException::class)
    fun resetCrcs() {
        flush()
        crc8 = 0
        crc16 = 0
    }

    // Returns the CRC-8 hash of all the bytes written since the last call to resetCrcs()
    // (or from the beginning of stream if reset was never called).
    @Throws(IOException::class)
    fun getCrc8(): Int {
        checkByteAligned()
        flush()
        if ((crc8 ushr 8) != 0) throw AssertionError()
        return crc8
    }

    // Returns the CRC-16 hash of all the bytes written since the last call to resetCrcs()
    // (or from the beginning of stream if reset was never called).
    @Throws(IOException::class)
    fun getCrc16(): Int {
        checkByteAligned()
        flush()
        if ((crc16 ushr 16) != 0) throw AssertionError()
        return crc16
    }

    /*-- Miscellaneous --*/ // Returns the number of bytes written since the start of the stream.
    fun getByteCount(): Long {
        return byteCount + bitBufferLen / 8
    }

    // Writes out any internally buffered bit data, closes the underlying output stream, and invalidates this
    // bit output stream object for any future operation. Note that a BitOutputStream only uses memory but
    // does not have native resources. It is okay to flush() the pending data and simply let a BitOutputStream
    // be garbage collected without calling close(), but the parent is still responsible for calling close()
    // on the underlying output stream if it uses native resources (such as FileOutputStream or SocketOutputStream).
    @Throws(IOException::class)
    public override fun close() {
        if (out != null) {
            checkByteAligned()
            flush()
            out!!.close()
            out = null
        }
    }
}
*/
