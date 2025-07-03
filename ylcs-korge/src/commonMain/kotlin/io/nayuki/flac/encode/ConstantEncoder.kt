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

/* 
 * Under the constant coding mode, this provides size calculations on and bitstream encoding of audio sample data.
 * Note that unlike the other subframe encoders which are fully general, not all data can be encoded using constant mode.
 * The encoded size depends on the shift and bit depth, but not on the data length or contents.
 */
internal class ConstantEncoder  // Constructs a constant encoder for the given data, right shift, and sample depth.
    (samples: LongArray?, shift: Int, depth: Int) : SubframeEncoder(shift, depth) {
    // Encodes the given vector of audio sample data to the given bit output stream using
    // the this encoding method (and the superclass fields sampleShift and sampleDepth).
    // This requires the data array to have the same values (but not necessarily
    // the same object reference) as the array that was passed to the constructor.
    @Throws(IOException::class)
    public override fun encode(samples: LongArray, out: BitOutputStream) {
        if (!isConstant(samples)) throw IllegalArgumentException("Data is not constant-valued")
        if (((samples.get(0) shr sampleShift) shl sampleShift) != samples.get(0)) throw IllegalArgumentException("Invalid shift value for data")
        writeTypeAndShift(0, out)
        writeRawSample(samples.get(0) shr sampleShift, out)
    }

    companion object {
        // Computes the best way to encode the given values under the constant coding mode,
        // returning an exact size plus a new encoder object associated with the input arguments.
        // However if the sample data is non-constant then null is returned instead,
        // to indicate that the data is impossible to represent in this mode.
        fun computeBest(samples: LongArray, shift: Int, depth: Int): SizeEstimate<SubframeEncoder?>? {
            if (!isConstant(samples)) return null
            val enc: ConstantEncoder = ConstantEncoder(samples, shift, depth)
            val size: Long = (1 + 6 + 1 + shift + depth).toLong()
            return SizeEstimate(size, enc)
        }

        // Returns true iff the set of unique values in the array has size exactly 1. Pure function.
        private fun isConstant(data: LongArray): Boolean {
            if (data.size == 0) return false
            val `val`: Long = data.get(0)
            for (x: Long in data) {
                if (x != `val`) return false
            }
            return true
        }
    }
}
*/
