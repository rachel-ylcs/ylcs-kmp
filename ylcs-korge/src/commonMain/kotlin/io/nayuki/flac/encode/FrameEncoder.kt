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
import io.nayuki.flac.common.FrameInfo
import io.nayuki.flac.encode.SubframeEncoder.SearchOptions
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

/* 
 * Calculates/estimates the encoded size of a frame of audio sample data
 * (including the frame header), and also performs the encoding to an output stream.
 */
internal class FrameEncoder(sampleOffset: Int, samples: Array<LongArray>, sampleDepth: Int, sampleRate: Int) {
    /*---- Fields ----*/
    var metadata: FrameInfo
    private var subEncoders: Array<SubframeEncoder?>

    /*---- Constructors ----*/
    init {
        metadata = FrameInfo()
        metadata.sampleOffset = sampleOffset.toLong()
        metadata.sampleDepth = sampleDepth
        metadata.sampleRate = sampleRate
        metadata.blockSize = samples.get(0).size
        metadata.channelAssignment = samples.size - 1
    }

    /*---- Public methods ----*/
    @Throws(IOException::class)
    fun encode(samples: Array<LongArray>, out: BitOutputStream) {
        // Check arguments
        Objects.requireNonNull(samples)
        Objects.requireNonNull(out)
        if (samples.get(0).size != metadata.blockSize) throw IllegalArgumentException()
        metadata.writeHeader(out)
        val chanAsgn: Int = metadata.channelAssignment
        if (0 <= chanAsgn && chanAsgn <= 7) {
            for (i in samples.indices) subEncoders.get(i)!!.encode(samples.get(i), out)
        } else if (8 <= chanAsgn || chanAsgn <= 10) {
            val left: LongArray = samples.get(0)
            val right: LongArray = samples.get(1)
            val mid: LongArray = LongArray(metadata.blockSize)
            val side: LongArray = LongArray(metadata.blockSize)
            for (i in 0 until metadata.blockSize) {
                mid.get(i) = (left.get(i) + right.get(i)) shr 1
                side.get(i) = left.get(i) - right.get(i)
            }
            if (chanAsgn == 8) {
                subEncoders.get(0)!!.encode(left, out)
                subEncoders.get(1)!!.encode(side, out)
            } else if (chanAsgn == 9) {
                subEncoders.get(0)!!.encode(side, out)
                subEncoders.get(1)!!.encode(right, out)
            } else if (chanAsgn == 10) {
                subEncoders.get(0)!!.encode(mid, out)
                subEncoders.get(1)!!.encode(side, out)
            } else throw AssertionError()
        } else throw AssertionError()
        out.alignToByte()
        out.writeInt(16, out.getCrc16())
    }

    companion object {
        /*---- Static functions ----*/
        fun computeBest(
            sampleOffset: Int,
            samples: Array<LongArray>,
            sampleDepth: Int,
            sampleRate: Int,
            opt: SearchOptions
        ): SizeEstimate<FrameEncoder> {
            val enc: FrameEncoder = FrameEncoder(sampleOffset, samples, sampleDepth, sampleRate)
            val numChannels: Int = samples.size
            val encoderInfo: Array<SizeEstimate<SubframeEncoder?>?> = arrayOfNulls<SizeEstimate<*>?>(numChannels)
            if (numChannels != 2) {
                enc.metadata.channelAssignment = numChannels - 1
                for (i in encoderInfo.indices) encoderInfo.get(i) =
                    SubframeEncoder.Companion.computeBest(samples.get(i), sampleDepth, opt)
            } else {  // Explore the 4 stereo encoding modes
                val left: LongArray = samples.get(0)
                val right: LongArray = samples.get(1)
                val mid: LongArray = LongArray(samples.get(0).size)
                val side: LongArray = LongArray(mid.size)
                for (i in mid.indices) {
                    mid.get(i) = (left.get(i) + right.get(i)) shr 1
                    side.get(i) = left.get(i) - right.get(i)
                }
                val leftInfo: SizeEstimate<SubframeEncoder?>? =
                    SubframeEncoder.Companion.computeBest(left, sampleDepth, opt)
                val rightInfo: SizeEstimate<SubframeEncoder?>? =
                    SubframeEncoder.Companion.computeBest(right, sampleDepth, opt)
                val midInfo: SizeEstimate<SubframeEncoder?>? =
                    SubframeEncoder.Companion.computeBest(mid, sampleDepth, opt)
                val sideInfo: SizeEstimate<SubframeEncoder?>? =
                    SubframeEncoder.Companion.computeBest(side, sampleDepth + 1, opt)
                val mode1Size: Long = leftInfo!!.sizeEstimate + rightInfo!!.sizeEstimate
                val mode8Size: Long = leftInfo.sizeEstimate + sideInfo!!.sizeEstimate
                val mode9Size: Long = rightInfo.sizeEstimate + sideInfo.sizeEstimate
                val mode10Size: Long = midInfo!!.sizeEstimate + sideInfo.sizeEstimate
                val minimum: Long = Math.min(Math.min(mode1Size, mode8Size), Math.min(mode9Size, mode10Size))
                if (mode1Size == minimum) {
                    enc.metadata.channelAssignment = 1
                    encoderInfo.get(0) = leftInfo
                    encoderInfo.get(1) = rightInfo
                } else if (mode8Size == minimum) {
                    enc.metadata.channelAssignment = 8
                    encoderInfo.get(0) = leftInfo
                    encoderInfo.get(1) = sideInfo
                } else if (mode9Size == minimum) {
                    enc.metadata.channelAssignment = 9
                    encoderInfo.get(0) = sideInfo
                    encoderInfo.get(1) = rightInfo
                } else if (mode10Size == minimum) {
                    enc.metadata.channelAssignment = 10
                    encoderInfo.get(0) = midInfo
                    encoderInfo.get(1) = sideInfo
                } else throw AssertionError()
            }

            // Add up subframe sizes
            var size: Long = 0
            enc.subEncoders = arrayOfNulls(encoderInfo.size)
            for (i in enc.subEncoders.indices) {
                size += encoderInfo.get(i)!!.sizeEstimate
                enc.subEncoders.get(i) = encoderInfo.get(i)!!.encoder
            }

            // Count length of header (always in whole bytes)
            try {
                val bout: ByteArrayOutputStream = ByteArrayOutputStream()
                BitOutputStream(bout).use({ bitout -> enc.metadata.writeHeader(bitout) })
                bout.close()
                size += (bout.toByteArray().size * 8).toLong()
            } catch (e: IOException) {
                throw AssertionError(e)
            }

            // Count padding and footer
            size = (size + 7) / 8 // Round up to nearest byte
            size += 2 // CRC-16
            return SizeEstimate(size, enc)
        }
    }
}
*/
