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
import io.nayuki.flac.common.StreamInfo
import io.nayuki.flac.encode.SubframeEncoder.SearchOptions
import java.util.*

class AdvancedFlacEncoder(
    info: StreamInfo,
    samples: Array<IntArray>,
    baseSize: Int,
    sizeMultiples: IntArray,
    opts: SearchOptions,
    out: BitOutputStream
) {
    init {
        val numSamples: Int = samples.get(0).size

        // Calculate compressed sizes for many block positions and sizes
        val encoderInfo: Array<Array<SizeEstimate<FrameEncoder>>> = Array<Array<SizeEstimate<*>>>(
            sizeMultiples.size,
            { arrayOfNulls<SizeEstimate<*>>((numSamples + baseSize - 1) / baseSize) })
        val startTime: Long = System.currentTimeMillis()
        for (i in encoderInfo.get(0).indices) {
            val progress: Double = i.toDouble() / encoderInfo.get(0).size
            val timeRemain: Double = (System.currentTimeMillis() - startTime) / 1000.0 / progress * (1 - progress)
            System.err.printf("\rprogress=%.2f%%    timeRemain=%ds", progress * 100, Math.round(timeRemain))
            val pos: Int = i * baseSize
            for (j in encoderInfo.indices) {
                val n: Int = Math.min(sizeMultiples.get(j) * baseSize, numSamples - pos)
                val subsamples: Array<LongArray> = getRange(samples, pos, n)
                encoderInfo.get(j).get(i) =
                    FrameEncoder.Companion.computeBest(pos, subsamples, info.sampleDepth, info.sampleRate, opts)
            }
        }
        System.err.println()

        // Initialize arrays to prepare for dynamic programming
        val bestEncoders: Array<FrameEncoder?> = arrayOfNulls(encoderInfo.get(0).size)
        val bestSizes: LongArray = LongArray(bestEncoders.size)
        Arrays.fill(bestSizes, Long.MAX_VALUE)

        // Use dynamic programming to calculate optimum block size switching
        for (i in encoderInfo.indices) {
            for (j in bestSizes.indices.reversed()) {
                var size: Long = encoderInfo.get(i).get(j).sizeEstimate
                if (j + sizeMultiples.get(i) < bestSizes.size) size += bestSizes.get(j + sizeMultiples.get(i))
                if (size < bestSizes.get(j)) {
                    bestSizes.get(j) = size
                    bestEncoders.get(j) = encoderInfo.get(i).get(j).encoder
                }
            }
        }

        // Do the actual encoding and writing
        info.minBlockSize = 0
        info.maxBlockSize = 0
        info.minFrameSize = 0
        info.maxFrameSize = 0
        val blockSizes: MutableList<Int> = ArrayList()
        var i: Int = 0
        while (i < bestEncoders.size) {
            val enc: FrameEncoder? = bestEncoders.get(i)
            val pos: Int = i * baseSize
            val n: Int = Math.min(enc!!.metadata.blockSize, numSamples - pos)
            blockSizes.add(n)
            if (info.minBlockSize == 0 || n < info.minBlockSize) info.minBlockSize = Math.max(n, 16)
            info.maxBlockSize = Math.max(n, info.maxBlockSize)
            val subsamples: Array<LongArray> = getRange(samples, pos, n)
            val startByte: Long = out.getByteCount()
            bestEncoders.get(i)!!.encode(subsamples, out)
            i += (n + baseSize - 1) / baseSize
            val frameSize: Long = out.getByteCount() - startByte
            if (frameSize < 0 || (frameSize.toInt()).toLong() != frameSize) throw AssertionError()
            if (info.minFrameSize == 0 || frameSize < info.minFrameSize) info.minFrameSize = frameSize.toInt()
            if (frameSize > info.maxFrameSize) info.maxFrameSize = frameSize.toInt()
        }
    }

    companion object {
        // Returns the subrange array[ : ][off : off + len] upcasted to long.
        private fun getRange(array: Array<IntArray>, off: Int, len: Int): Array<LongArray> {
            val result: Array<LongArray> = Array(array.size, { LongArray(len) })
            for (i in array.indices) {
                val src: IntArray = array.get(i)
                val dest: LongArray = result.get(i)
                for (j in 0 until len) dest.get(j) = src.get(off + j).toLong()
            }
            return result
        }
    }
}
*/
