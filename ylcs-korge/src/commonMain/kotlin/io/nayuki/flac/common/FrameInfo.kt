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
package io.nayuki.flac.common

import io.nayuki.flac.decode.*
import korlibs.io.lang.*

/**
 * Represents most fields in a frame header, in decoded (not raw) form. Mutable structure,
 * not thread safe. Also has methods for parsing and serializing this structure to/from bytes.
 * All fields can be modified freely when no method call is active.
 * @see FrameDecoder
 *
 * @see StreamInfo.checkFrame
 */
class FrameInfo {
    /*---- Fields ----*/ // Exactly one of these following two fields equals -1.
    /**
     * The index of this frame, where the foremost frame has index 0 and each subsequent frame
     * increments it. This is either a uint31 value or 1 if unused. Exactly one of the fields
     * frameIndex and sampleOffse is equal to 1 (not both nor neither). This value can only
     * be used if the stream info's minBlockSize = maxBlockSize (constant block size encoding style).
     */
    var frameIndex: Int = -1

    /**
     * The offset of the first sample in this frame with respect to the beginning of the
     * audio stream. This is either a uint36 value or 1 if unused. Exactly one of
     * the fields frameIndex and sampleOffse is equal to 1 (not both nor neither).
     */
    var sampleOffset: Long = -1

    /**
     * The number of audio channels in this frame, in the range 1 to 8 inclusive.
     * This value is fully determined by the channelAssignment field.
     */
    var numChannels: Int = -1

    /**
     * The raw channel assignment value of this frame, which is a uint4 value.
     * This indicates the number of channels, but also tells the stereo coding mode.
     */
    var channelAssignment: Int = -1

    /**
     * The number of samples per channel in this frame, in the range 1 to 65536 inclusive.
     */
    var blockSize: Int = -1

    /**
     * The sample rate of this frame in hertz (Hz), in the range 1 to 655360 inclusive,
     * or 1 if unavailable (i.e. the stream info should be consulted).
     */
    var sampleRate: Int = -1

    /**
     * The sample depth of this frame in bits, in the range 8 to 24 inclusive,
     * or 1 if unavailable (i.e. the stream info should be consulted).
     */
    var sampleDepth: Int = -1

    /**
     * The size of this frame in bytes, from the start of the sync sequence to the end
     * of the trailing CRC-16 checksum. A valid value is at least 10, or 1
     * if unavailable (e.g. the frame header was parsed but not the entire frame).
     */
    var frameSize: Int = -1

    /*---- Constructors ----*/ /**
     * Constructs a blank frame metadata structure, setting all fields to unknown or invalid values.
     */

    /*
    /*---- Functions to write FrameInfo to stream ----*/
    /**
     * Writes the current state of this object as a frame header to the specified
     * output stream, from the sync field through to the CRC-8 field (inclusive).
     * This does not write the data of subframes, the bit padding, nor the CRC-16 field.
     *
     * The stream must be byte-aligned before this method is called, and will be aligned
     * upon returning (i.e. it writes a whole number of bytes). This method initially resets
     * the stream's CRC computations, which is useful behavior for the caller because
     * it will need to write the CRC-16 at the end of the frame.
     * @param out the output stream to write to (not `null`)
     * @throws NullPointerException if the output stream is `null`
     * @throws IOException if an I/O exception occurred
     */
    @Throws(IOException::class)
    fun writeHeader(out: BitOutputStream) {
        out.resetCrcs()
        out.writeInt(14, 0x3FFE) // Sync
        out.writeInt(1, 0) // Reserved
        out.writeInt(1, 1) // Blocking strategy
        val blockSizeCode = getBlockSizeCode(blockSize)
        out.writeInt(4, blockSizeCode)
        val sampleRateCode = getSampleRateCode(sampleRate)
        out.writeInt(4, sampleRateCode)
        out.writeInt(4, channelAssignment)
        out.writeInt(3, getSampleDepthCode(sampleDepth))
        out.writeInt(1, 0) // Reserved

        // Variable-length: 1 to 7 bytes
        if (frameIndex != -1 && sampleOffset == -1L) writeUtf8Integer(
            sampleOffset,
            out
        ) else if (sampleOffset != -1L && frameIndex == -1) writeUtf8Integer(
            sampleOffset,
            out
        ) else throw IllegalStateException()

        // Variable-length: 0 to 2 bytes
        if (blockSizeCode == 6) out.writeInt(8, blockSize - 1) else if (blockSizeCode == 7) out.writeInt(
            16,
            blockSize - 1
        )

        // Variable-length: 0 to 2 bytes
        if (sampleRateCode == 12) out.writeInt(8, sampleRate) else if (sampleRateCode == 13) out.writeInt(
            16,
            sampleRate
        ) else if (sampleRateCode == 14) out.writeInt(16, sampleRate / 10)
        out.writeInt(8, out.crc8)
    }
    */

    companion object {
        /*---- Functions to read FrameInfo from stream ----*/
        /**
         * Reads the next FLAC frame header from the specified input stream, either returning
         * a new frame info object or `null`. The stream must be aligned to a byte
         * boundary and start at a sync sequence. If EOF is immediately encountered before
         * any bytes were read, then this returns `null`.
         *
         * Otherwise this reads between 6 to 16 bytes from the stream  starting
         * from the sync code, and ending after the CRC-8 value is read (but before reading
         * any subframes). It tries to parse the frame header data. After the values are
         * successfully decoded, a new frame info object is created, almost all fields are
         * set to the parsed values, and it is returned. (This doesn't read to the end
         * of the frame, so the frameSize field is set to -1.)
         * @param in the input stream to read from (not `null`)
         * @return a new frame info object or `null`
         * @throws NullPointerException if the input stream is `null`
         * @throws DataFormatException if the input data contains invalid values
         * @throws IOException if an I/O exception occurred
         */
        @Throws(IOException::class)
        fun readFrame(`in`: FlacLowLevelInput?): FrameInfo? {
            // Preliminaries
            `in`!!.resetCrcs()
            val temp = `in`.readByte()
            if (temp == -1) return null
            val result = FrameInfo()
            result.frameSize = -1

            // Read sync bits
            val sync = temp shl 6 or `in`.readUint(6) // Uint14
            if (sync != 0x3FFE) throw DataFormatException("Sync code expected")

            // Read various simple fields
            if (`in`.readUint(1) != 0) throw DataFormatException("Reserved bit")
            val blockStrategy = `in`.readUint(1)
            val blockSizeCode = `in`.readUint(4)
            val sampleRateCode = `in`.readUint(4)
            val chanAsgn = `in`.readUint(4)
            result.channelAssignment = chanAsgn
            if (chanAsgn < 8) result.numChannels =
                chanAsgn + 1 else if (8 <= chanAsgn && chanAsgn <= 10) result.numChannels =
                2 else throw DataFormatException("Reserved channel assignment")
            result.sampleDepth = decodeSampleDepth(`in`.readUint(3))
            if (`in`.readUint(1) != 0) throw DataFormatException("Reserved bit")

            // Read and check the frame/sample position field
            val position = readUtf8Integer(`in`) // Reads 1 to 7 bytes
            if (blockStrategy == 0) {
                if (position ushr 31 != 0L) throw DataFormatException("Frame index too large")
                result.frameIndex = position.toInt()
                result.sampleOffset = -1
            } else if (blockStrategy == 1) {
                result.sampleOffset = position
                result.frameIndex = -1
            } else throw AssertionError()

            // Read variable-length data for some fields
            result.blockSize = decodeBlockSize(blockSizeCode, `in`) // Reads 0 to 2 bytes
            result.sampleRate = decodeSampleRate(sampleRateCode, `in`) // Reads 0 to 2 bytes
            val computedCrc8 = `in`.getCrc8()
            if (`in`.readUint(8) != computedCrc8) throw DataFormatException("CRC-8 mismatch")
            return result
        }

        // Reads 1 to 7 whole bytes from the input stream. Return value is a uint36.
        // See: https://hydrogenaud.io/index.php/topic,112831.msg929128.html#msg929128
        @Throws(IOException::class)
        private fun readUtf8Integer(`in`: FlacLowLevelInput?): Long {
            val head = `in`!!.readUint(8)
            val n = ((head shl 24).inv()).countLeadingZeroBits() // Number of leading 1s in the byte
            assert(n in 0..8)
            return if (n == 0) head.toLong() else if (n == 1 || n == 8) throw DataFormatException("Invalid UTF-8 coded number") else {
                var result = (head and (0x7F ushr n)).toLong()
                for (i in 0 until n - 1) {
                    val temp = `in`.readUint(8)
                    if (temp and 0xC0 != 0x80) throw DataFormatException("Invalid UTF-8 coded number")
                    result = result shl 6 or (temp and 0x3F).toLong()
                }
                if (result ushr 36 != 0L) throw AssertionError()
                result
            }
        }

        // Argument is a uint4 value. Reads 0 to 2 bytes from the input stream.
        // Return value is in the range [1, 65536].
        @Throws(IOException::class)
        private fun decodeBlockSize(code: Int, `in`: FlacLowLevelInput?): Int {
            require(code ushr 4 == 0)
            return when (code) {
                0 -> throw DataFormatException("Reserved block size")
                6 -> `in`!!.readUint(8) + 1
                7 -> `in`!!.readUint(16) + 1
                else -> {
                    val result = searchSecond(
                        BLOCK_SIZE_CODES,
                        code
                    )
                    if (result < 1 || result > 65536) throw AssertionError()
                    result
                }
            }
        }

        // Argument is a uint4 value. Reads 0 to 2 bytes from the input stream.
        // Return value is in the range [-1, 655350].
        @Throws(IOException::class)
        private fun decodeSampleRate(code: Int, `in`: FlacLowLevelInput?): Int {
            require(code ushr 4 == 0)
            return when (code) {
                0 -> -1 // Caller should obtain value from stream info metadata block
                12 -> `in`!!.readUint(8)
                13 -> `in`!!.readUint(16)
                14 -> `in`!!.readUint(16) * 10
                15 -> throw DataFormatException("Invalid sample rate")
                else -> {
                    val result = searchSecond(
                        SAMPLE_RATE_CODES,
                        code
                    )
                    if (result < 1 || result > 655350) throw AssertionError()
                    result
                }
            }
        }

        // Argument is a uint3 value. Pure function and performs no I/O. Return value is in the range [-1, 24].
        private fun decodeSampleDepth(code: Int): Int {
            return if (code ushr 3 != 0) throw IllegalArgumentException() else if (code == 0) -1 // Caller should obtain value from stream info metadata block
            else {
                val result = searchSecond(
                    SAMPLE_DEPTH_CODES,
                    code
                )
                if (result == -1) throw DataFormatException("Reserved bit depth")
                if (result < 1 || result > 32) throw AssertionError()
                result
            }
        }

        // Given a uint36 value, this writes 1 to 7 whole bytes to the given output stream.
        //@Throws(IOException::class)
        //private fun writeUtf8Integer(`val`: Long, out: BitOutputStream) {
        //    require(`val` ushr 36 == 0L)
        //    val bitLen = 64 - `val`.countLeadingZeroBits()
        //    if (bitLen <= 7) out.writeInt(8, `val`.toInt()) else {
        //        val n = (bitLen - 2) / 5
        //        out.writeInt(8, 0xFF80 ushr n or (`val` ushr n * 6).toInt())
        //        for (i in n - 1 downTo 0) out.writeInt(8, 0x80 or ((`val` ushr i * 6).toInt() and 0x3F))
        //    }
        //}

        // Returns a uint4 value representing the given block size. Pure function.
        private fun getBlockSizeCode(blockSize: Int): Int {
            var result = searchFirst(BLOCK_SIZE_CODES, blockSize)
            if (result != -1) ; else if (1 <= blockSize && blockSize <= 256) result =
                6 else if (1 <= blockSize && blockSize <= 65536) result = 7 else  // blockSize < 1 || blockSize > 65536
                throw IllegalArgumentException()
            if (result ushr 4 != 0) throw AssertionError()
            return result
        }

        // Returns a uint4 value representing the given sample rate. Pure function.
        private fun getSampleRateCode(sampleRate: Int): Int {
            require(!(sampleRate == 0 || sampleRate < -1))
            var result = searchFirst(SAMPLE_RATE_CODES, sampleRate)
            if (result != -1) ; else if (0 <= sampleRate && sampleRate < 256) result =
                12 else if (0 <= sampleRate && sampleRate < 65536) result =
                13 else if (0 <= sampleRate && sampleRate < 655360 && sampleRate % 10 == 0) result = 14 else result = 0
            if (result ushr 4 != 0) throw AssertionError()
            return result
        }

        // Returns a uint3 value representing the given sample depth. Pure function.
        private fun getSampleDepthCode(sampleDepth: Int): Int {
            require(!(sampleDepth != -1 && (sampleDepth < 1 || sampleDepth > 32)))
            var result = searchFirst(SAMPLE_DEPTH_CODES, sampleDepth)
            if (result == -1) result = 0
            if (result ushr 3 != 0) throw AssertionError()
            return result
        }

        /*---- Tables of constants and search functions ----*/
        private fun searchFirst(table: Array<IntArray>, key: Int): Int {
            for (pair in table) {
                if (pair[0] == key) return pair[1]
            }
            return -1
        }

        private fun searchSecond(table: Array<IntArray>, key: Int): Int {
            for (pair in table) {
                if (pair[1] == key) return pair[0]
            }
            return -1
        }

        private val BLOCK_SIZE_CODES = arrayOf(
            intArrayOf(192, 1),
            intArrayOf(576, 2),
            intArrayOf(1152, 3),
            intArrayOf(2304, 4),
            intArrayOf(4608, 5),
            intArrayOf(256, 8),
            intArrayOf(512, 9),
            intArrayOf(1024, 10),
            intArrayOf(2048, 11),
            intArrayOf(4096, 12),
            intArrayOf(8192, 13),
            intArrayOf(16384, 14),
            intArrayOf(32768, 15)
        )
        private val SAMPLE_DEPTH_CODES =
            arrayOf(intArrayOf(8, 1), intArrayOf(12, 2), intArrayOf(16, 4), intArrayOf(20, 5), intArrayOf(24, 6))
        private val SAMPLE_RATE_CODES = arrayOf(
            intArrayOf(88200, 1),
            intArrayOf(176400, 2),
            intArrayOf(192000, 3),
            intArrayOf(8000, 4),
            intArrayOf(16000, 5),
            intArrayOf(22050, 6),
            intArrayOf(24000, 7),
            intArrayOf(32000, 8),
            intArrayOf(44100, 9),
            intArrayOf(48000, 10),
            intArrayOf(96000, 11)
        )
    }
}
