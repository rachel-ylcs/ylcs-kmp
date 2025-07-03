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

class FlacEncoder(
    info: StreamInfo,
    samples: Array<IntArray>,
    blockSize: Int,
    opt: SearchOptions,
    out: BitOutputStream
) {
    init {
        info.minBlockSize = blockSize
        info.maxBlockSize = blockSize
        info.minFrameSize = 0
        info.maxFrameSize = 0
        var i = 0
        var pos = 0
        while (pos < samples[0].size) {
            System.err.printf("frame=%d  position=%d  %.2f%%%n", i, pos, 100.0 * pos / samples[0].size)
            val n = Math.min(samples[0].size - pos, blockSize)
            val subsamples = getRange(samples, pos, n)
            val enc: FrameEncoder =
                FrameEncoder.Companion.computeBest(pos, subsamples, info.sampleDepth, info.sampleRate, opt).encoder
            val startByte = out.byteCount
            enc.encode(subsamples, out)
            val frameSize = out.byteCount - startByte
            if (frameSize < 0 || frameSize.toInt().toLong() != frameSize) throw AssertionError()
            if (info.minFrameSize == 0 || frameSize < info.minFrameSize) info.minFrameSize = frameSize.toInt()
            if (frameSize > info.maxFrameSize) info.maxFrameSize = frameSize.toInt()
            pos += n
            i++
        }
    }

    companion object {
        // Returns the subrange array[ : ][off : off + len] upcasted to long.
        private fun getRange(array: Array<IntArray>, off: Int, len: Int): Array<LongArray> {
            val result = Array(array.size) { LongArray(len) }
            for (i in array.indices) {
                val src = array[i]
                val dest = result[i]
                for (j in 0 until len) dest[j] = src[off + j].toLong()
            }
            return result
        }
    }
}
*/
