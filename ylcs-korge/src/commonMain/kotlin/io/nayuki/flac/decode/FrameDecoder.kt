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

import io.nayuki.flac.common.FrameInfo
import korlibs.io.lang.*

/**
 * Decodes a FLAC frame from an input stream into raw audio samples. Note that these objects are
 * stateful and not thread-safe, due to the bit input stream field, private temporary arrays, etc.
 *
 * This class only uses memory and has no native resources; however, the
 * code that uses this class is responsible for cleaning up the input stream.
 * @see FlacDecoder
 *
 * @see FlacLowLevelInput
 */
class FrameDecoder(/*---- Fields ----*/ // Can be changed when there is no active call of readFrame().
    // Must be not null when readFrame() is called.
                   var `in`: FlacLowLevelInput?, // Can be changed when there is no active call of readFrame().
    // Must be in the range [4, 32].
                   var expectedSampleDepth: Int
) {
    // Temporary arrays to hold two decoded audio channels (a.k.a. subframes). They have int64 range
    // because the worst case of 32-bit audio encoded in stereo side mode uses signed 33 bits.
    // The maximum possible block size is either 65536 samples per channel from the
    // frame header logic, or 65535 from a strict reading of the FLAC specification.
    // Two buffers are needed for stereo coding modes, but not more than two because
    // all other multi-channel audio is processed independently per channel.
    private val temp0: LongArray
    private val temp1: LongArray

    // The number of samples (per channel) in the current block/frame being processed.
    // This value is only valid while the method readFrame() is on the call stack.
    // When readFrame() is active, this value is in the range [1, 65536].
    private var currentBlockSize: Int

    /*---- Methods ----*/ // Reads the next frame of FLAC data from the current bit input stream, decodes it,
    // and stores output samples into the given array, and returns a new metadata object.
    // The bit input stream must be initially aligned at a byte boundary. If EOF is encountered before
    // any actual bytes were read, then this returns null. Otherwise this function either successfully
    // decodes a frame and returns a new metadata object, or throws an appropriate exception. A frame
    // may have up to 8 channels and 65536 samples, so the output arrays need to be sized appropriately.
    fun readFrame(outSamples: Array<IntArray>, outOffset: Int): FrameInfo? {
        // Check field states
        check(currentBlockSize == -1) { "Concurrent call" }

        // Parse the frame header to see if one is available
        val startByte = `in`!!.position
        val meta: FrameInfo = FrameInfo.Companion.readFrame(`in`) ?: // EOF occurred cleanly
        return null
        if (meta.sampleDepth != -1 && meta.sampleDepth != expectedSampleDepth) throw DataFormatException("Sample depth mismatch")

        // Check arguments and read frame header
        currentBlockSize = meta.blockSize
        if (outOffset < 0 || outOffset > outSamples[0].size) throw IndexOutOfBoundsException()
        require(outSamples.size >= meta.numChannels) { "Output array too small for number of channels" }
        if (outOffset > outSamples[0].size - currentBlockSize) throw IndexOutOfBoundsException()

        // Do the hard work
        decodeSubframes(expectedSampleDepth, meta.channelAssignment, outSamples, outOffset)

        // Read padding and footer
        if (`in`!!.readUint((8 - `in`!!.bitPosition) % 8) != 0) throw DataFormatException("Invalid padding bits")
        val computedCrc16 = `in`!!.getCrc16()
        if (`in`!!.readUint(16) != computedCrc16) throw DataFormatException("CRC-16 mismatch")

        // Handle frame size and miscellaneous
        val frameSize = `in`!!.position - startByte
        if (frameSize < 10) throw AssertionError()
        if (frameSize.toInt().toLong() != frameSize) throw DataFormatException("Frame size too large")
        meta.frameSize = frameSize.toInt()
        currentBlockSize = -1
        return meta
    }

    // Based on the current bit input stream and the two given arguments, this method reads and decodes
    // each subframe, performs stereo decoding if applicable, and writes the final uncompressed audio data
    // to the array range outSamples[0 : numChannels][outOffset : outOffset + currentBlockSize].
    // Note that this method uses the private temporary arrays and passes them into sub-method calls.
    private fun decodeSubframes(sampleDepth: Int, chanAsgn: Int, outSamples: Array<IntArray>, outOffset: Int) {
        // Check arguments
        require(!(sampleDepth < 1 || sampleDepth > 32))
        require(chanAsgn ushr 4 == 0)
        if (0 <= chanAsgn && chanAsgn <= 7) {
            // Handle 1 to 8 independently coded channels
            val numChannels = chanAsgn + 1
            for (ch in 0 until numChannels) {
                decodeSubframe(sampleDepth, temp0)
                val outChan = outSamples[ch]
                for (i in 0 until currentBlockSize) outChan[outOffset + i] = checkBitDepth(
                    temp0[i], sampleDepth
                )
            }
        } else if (8 <= chanAsgn && chanAsgn <= 10) {
            // Handle one of the side-coded stereo methods
            decodeSubframe(sampleDepth + if (chanAsgn == 9) 1 else 0, temp0)
            decodeSubframe(sampleDepth + if (chanAsgn == 9) 0 else 1, temp1)
            if (chanAsgn == 8) {  // Left-side stereo
                for (i in 0 until currentBlockSize) temp1[i] = temp0[i] - temp1[i]
            } else if (chanAsgn == 9) {  // Side-right stereo
                for (i in 0 until currentBlockSize) temp0[i] += temp1[i]
            } else if (chanAsgn == 10) {  // Mid-side stereo
                for (i in 0 until currentBlockSize) {
                    val side = temp1[i]
                    val right = temp0[i] - (side shr 1)
                    temp1[i] = right
                    temp0[i] = right + side
                }
            } else throw AssertionError()

            // Copy data from temporary to output arrays, and convert from long to int
            val outLeft = outSamples[0]
            val outRight = outSamples[1]
            for (i in 0 until currentBlockSize) {
                outLeft[outOffset + i] = checkBitDepth(temp0[i], sampleDepth)
                outRight[outOffset + i] = checkBitDepth(temp1[i], sampleDepth)
            }
        } else  // 11 <= channelAssignment <= 15
            throw DataFormatException("Reserved channel assignment")
    }

    // Reads one subframe from the bit input stream, decodes it, and writes to result[0 : currentBlockSize].
    private fun decodeSubframe(sampleDepth: Int, result: LongArray) {
        // Check arguments
        var sampleDepth = sampleDepth
        require(!(sampleDepth < 1 || sampleDepth > 33))
        require(result.size >= currentBlockSize)

        // Read header fields
        if (`in`!!.readUint(1) != 0) throw DataFormatException("Invalid padding bit")
        val type = `in`!!.readUint(6)
        var shift = `in`!!.readUint(1) // Also known as "wasted bits-per-sample"
        if (shift == 1) {
            while (`in`!!.readUint(1) == 0) {  // Unary coding
                if (shift >= sampleDepth) throw DataFormatException("Waste-bits-per-sample exceeds sample depth")
                shift++
            }
        }
        if (!(0 <= shift && shift <= sampleDepth)) throw AssertionError()
        sampleDepth -= shift

        // Read sample data based on type
        when (type) {
            0 -> { // Constant coding
                result.fill(`in`!!.readSignedInt(sampleDepth).toLong()!!, 0, currentBlockSize)
            }
            1 -> {  // Verbatim coding
                for (i in 0 until currentBlockSize) result[i] = `in`!!.readSignedInt(sampleDepth).toLong()
            }
            in 8..12 -> decodeFixedPredictionSubframe(
                type - 8,
                sampleDepth,
                result
            )
            in 32..63 -> decodeLinearPredictiveCodingSubframe(
                type - 31,
                sampleDepth,
                result
            )
            else -> throw DataFormatException("Reserved subframe type")
        }

        // Add trailing zeros to each sample
        if (shift > 0) {
            for (i in 0 until currentBlockSize) result[i] = result[i] shl shift
        }
    }

    // Reads from the input stream, performs computation, and writes to result[0 : currentBlockSize].
    private fun decodeFixedPredictionSubframe(predOrder: Int, sampleDepth: Int, result: LongArray) {
        // Check arguments
        require(!(sampleDepth < 1 || sampleDepth > 33))
        require(!(predOrder < 0 || predOrder >= FIXED_PREDICTION_COEFFICIENTS.size))
        if (predOrder > currentBlockSize) throw DataFormatException("Fixed prediction order exceeds block size")
        require(result.size >= currentBlockSize)

        // Read and compute various values
        for (i in 0 until predOrder)  // Non-Rice-coded warm-up samples
            result[i] = `in`!!.readSignedInt(sampleDepth).toLong()
        readResiduals(predOrder, result)
        restoreLpc(result, FIXED_PREDICTION_COEFFICIENTS[predOrder], sampleDepth, 0)
    }

    /*---- Constructors ----*/ // Constructs a frame decoder that initially uses the given stream.
    // The caller is responsible for cleaning up the input stream.
    init {
        temp0 = LongArray(65536)
        temp1 = LongArray(65536)
        currentBlockSize = -1
    }

    // Reads from the input stream, performs computation, and writes to result[0 : currentBlockSize].
    private fun decodeLinearPredictiveCodingSubframe(lpcOrder: Int, sampleDepth: Int, result: LongArray) {
        // Check arguments
        require(!(sampleDepth < 1 || sampleDepth > 33))
        require(!(lpcOrder < 1 || lpcOrder > 32))
        if (lpcOrder > currentBlockSize) throw DataFormatException("LPC order exceeds block size")
        require(result.size >= currentBlockSize)

        // Read non-Rice-coded warm-up samples
        for (i in 0 until lpcOrder) result[i] = `in`!!.readSignedInt(sampleDepth).toLong()

        // Read parameters for the LPC coefficients
        val precision = `in`!!.readUint(4) + 1
        if (precision == 16) throw DataFormatException("Invalid LPC precision")
        val shift = `in`!!.readSignedInt(5)
        if (shift < 0) throw DataFormatException("Invalid LPC shift")

        // Read the coefficients themselves
        val coefs = IntArray(lpcOrder)
        for (i in coefs.indices) coefs[i] = `in`!!.readSignedInt(precision)

        // Perform the main LPC decoding
        readResiduals(lpcOrder, result)
        restoreLpc(result, coefs, sampleDepth, shift)
    }

    // Updates the values of result[coefs.length : currentBlockSize] according to linear predictive coding.
    // This method reads all the arguments and the field currentBlockSize, only writes to result, and has no other side effects.
    // After this method returns, every value in result must fit in a signed sampleDepth-bit integer.
    // The largest allowed sample depth is 33, hence the largest absolute value allowed in the result is 2^32.
    // During the LPC restoration process, the prefix of result before index i consists of entirely int33 values.
    // Because coefs.length <= 32 and each coefficient fits in a signed int15 (both according to the FLAC specification),
    // the maximum (worst-case) absolute value of 'sum' is 2^32 * 2^14 * 32 = 2^51, which fits in a signed int53.
    // And because of this, the maximum possible absolute value of a residual before LPC restoration is applied,
    // such that the post-LPC result fits in a signed int33, is 2^51 + 2^32 which also fits in a signed int53.
    // Therefore a residue that is larger than a signed int53 will necessarily not fit in the int33 result and is wrong.
    private fun restoreLpc(result: LongArray, coefs: IntArray, sampleDepth: Int, shift: Int) {
        // Check and handle arguments
        require(result.size >= currentBlockSize)
        require(!(sampleDepth < 1 || sampleDepth > 33))
        require(!(shift < 0 || shift > 63))
        val lowerBound = (-1 shl sampleDepth - 1).toLong()
        val upperBound = -(lowerBound + 1)
        for (i in coefs.size until currentBlockSize) {
            var sum: Long = 0
            for (j in coefs.indices) sum += result[i - 1 - j] * coefs[j]
            assert(
                sum shr 53 == 0L || sum shr 53 == -1L // Fits in signed int54
            )
            sum = result[i] + (sum shr shift)
            // Check that sum fits in a sampleDepth-bit signed integer,
            // i.e. -(2^(sampleDepth-1)) <= sum < 2^(sampleDepth-1)
            if (sum < lowerBound || sum > upperBound) throw DataFormatException("Post-LPC result exceeds bit depth")
            result[i] = sum
        }
    }

    // Reads metadata and Rice-coded numbers from the input stream, storing them in result[warmup : currentBlockSize].
    // The stored numbers are guaranteed to fit in a signed int53 - see the explanation in restoreLpc().
    private fun readResiduals(warmup: Int, result: LongArray) {
        // Check and handle arguments
        require(!(warmup < 0 || warmup > currentBlockSize))
        require(result.size >= currentBlockSize)
        val method = `in`!!.readUint(2)
        if (method >= 2) throw DataFormatException("Reserved residual coding method")
        assert(method == 0 || method == 1)
        val paramBits = if (method == 0) 4 else 5
        val escapeParam = if (method == 0) 0xF else 0x1F
        val partitionOrder = `in`!!.readUint(4)
        val numPartitions = 1 shl partitionOrder
        if (currentBlockSize % numPartitions != 0) throw DataFormatException("Block size not divisible by number of Rice partitions")
        val inc = currentBlockSize ushr partitionOrder
        var partEnd = inc
        var resultIndex = warmup
        while (partEnd <= currentBlockSize) {
            val param = `in`!!.readUint(paramBits)
            if (param == escapeParam) {
                val numBits = `in`!!.readUint(5)
                while (resultIndex < partEnd) {
                    result[resultIndex] = `in`!!.readSignedInt(numBits).toLong()
                    resultIndex++
                }
            } else {
                `in`!!.readRiceSignedInts(param, result, resultIndex, partEnd)
                resultIndex = partEnd
            }
            partEnd += inc
        }
    }

    companion object {
        // Checks that 'val' is a signed 'depth'-bit integer, and either returns the
        // value downcasted to an int or throws an exception if it's out of range.
        // Note that depth must be in the range [1, 32] because the return value is an int.
        // For example when depth = 16, the range of valid values is [-32768, 32767].
        private fun checkBitDepth(`val`: Long, depth: Int): Int {
            assert(1 <= depth && depth <= 32)
            // Equivalent check: (val >> (depth - 1)) == 0 || (val >> (depth - 1)) == -1
            return if (`val` shr depth - 1 == `val` shr depth) `val`.toInt() else throw IllegalArgumentException(
                "$`val` is not a signed $depth-bit value"
            )
        }

        private val FIXED_PREDICTION_COEFFICIENTS =
            arrayOf(intArrayOf(), intArrayOf(1), intArrayOf(2, -1), intArrayOf(3, -3, 1), intArrayOf(4, -6, 4, -1))
    }
}
