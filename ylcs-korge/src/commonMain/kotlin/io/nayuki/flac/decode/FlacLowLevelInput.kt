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
 * A low-level input stream tailored to the needs of FLAC decoding. An overview of methods includes
 * bit reading, CRC calculation, Rice decoding, and positioning and seeking (partly optional).
 * @see SeekableFileFlacInput
 *
 * @see FrameDecoder
 */
interface FlacLowLevelInput : AutoCloseable {
    /*---- Stream position ----*/ // Returns the total number of bytes in the FLAC file represented by this input stream.
    // This number should not change for the lifetime of this object. Implementing this is optional;
    // it's intended to support blind seeking without the use of seek tables, such as binary searching
    // the whole file. A class may choose to throw UnsupportedOperationException instead,
    // such as for a non-seekable network input stream of unknown length.
    val length: Long

    // Returns the current byte position in the stream, a non-negative value.
    // This increments after every 8 bits read, and a partially read byte is treated as unread.
    // This value is 0 initially, is set directly by seekTo(), and potentially increases
    // after every call to a read*() method. Other methods do not affect this value.
    val position: Long

    // Returns the current number of consumed bits in the current byte. This starts at 0,
    // increments for each bit consumed, maxes out at 7, then resets to 0 and repeats.
    val bitPosition: Int

    // Changes the position of the next read to the given byte offset from the start of the stream.
    // This also resets CRCs and sets the bit position to 0.
    // Implementing this is optional; it is intended to support playback seeking.
    // A class may choose to throw UnsupportedOperationException instead.
    @Throws(IOException::class)
    fun seekTo(pos: Long)

    /*---- Reading bitwise integers ----*/ // Reads the next given number of bits (0 <= n <= 32) as an unsigned integer (i.e. zero-extended to int32).
    // However in the case of n = 32, the result will be a signed integer that represents a uint32.
    @Throws(IOException::class)
    fun readUint(n: Int): Int

    // Reads the next given number of bits (0 <= n <= 32) as an signed integer (i.e. sign-extended to int32).
    @Throws(IOException::class)
    fun readSignedInt(n: Int): Int

    // Reads and decodes the next batch of Rice-coded signed integers. Note that any Rice-coded integer might read a large
    // number of bits from the underlying stream (but not in practice because it would be a very inefficient encoding).
    // Every new value stored into the array is guaranteed to fit into a signed int53 - see FrameDecoder.restoreLpc()
    // for an explanation of why this is a necessary (but not sufficient) bound on the range of decoded values.
    @Throws(IOException::class)
    fun readRiceSignedInts(param: Int, result: LongArray, start: Int, end: Int)

    /*---- Reading bytes ----*/ // Returns the next unsigned byte value (in the range [0, 255]) or -1 for EOF.
    // Must be called at a byte boundary (i.e. getBitPosition() == 0), otherwise IllegalStateException is thrown.
    @Throws(IOException::class)
    fun readByte(): Int

    // Discards any partial bits, then reads the given array fully or throws EOFException.
    // Must be called at a byte boundary (i.e. getBitPosition() == 0), otherwise IllegalStateException is thrown.
    @Throws(IOException::class)
    fun readFully(b: ByteArray)

    /*---- CRC calculations ----*/ // Marks the current byte position as the start of both CRC calculations.
    // The effect of resetCrcs() is implied at the beginning of stream and when seekTo() is called.
    // Must be called at a byte boundary (i.e. getBitPosition() == 0), otherwise IllegalStateException is thrown.
    fun resetCrcs()

    // Returns the CRC-8 hash of all the bytes read since the most recent time one of these
    // events occurred: a call to resetCrcs(), a call to seekTo(), the beginning of stream.
    // Must be called at a byte boundary (i.e. getBitPosition() == 0), otherwise IllegalStateException is thrown.
    fun getCrc8(): Int

    // Returns the CRC-16 hash of all the bytes read since the most recent time one of these
    // events occurred: a call to resetCrcs(), a call to seekTo(), the beginning of stream.
    // Must be called at a byte boundary (i.e. getBitPosition() == 0), otherwise IllegalStateException is thrown.
    fun getCrc16(): Int

    /*---- Miscellaneous ----*/ // Closes underlying objects / native resources, and possibly discards memory buffers.
    // Generally speaking, this operation invalidates this input stream, so calling methods
    // (other than close()) or accessing fields thereafter should be forbidden.
    // The close() method must be idempotent and safe when called more than once.
    // If an implementation does not have native or time-sensitive resources, it is okay for the class user
    // to skip calling close() and simply let the object be garbage-collected. But out of good habit, it is
    // recommended to always close a FlacLowLevelInput stream so that the logic works correctly on all types.
    override fun close()
}
