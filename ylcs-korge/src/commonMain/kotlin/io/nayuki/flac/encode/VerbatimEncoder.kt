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
/*
 * Under the verbatim coding mode, this provides size calculations on and bitstream encoding of audio sample data.
 * Note that the size depends on the data length, shift, and bit depth, but not on the data contents.
 */
internal class VerbatimEncoder  // Constructs a constant encoder for the given data, right shift, and sample depth.
    (samples: LongArray?, shift: Int, depth: Int) : SubframeEncoder(shift, depth) {
    // Encodes the given vector of audio sample data to the given bit output stream using
    // the this encoding method (and the superclass fields sampleShift and sampleDepth).
    // This requires the data array to have the same values (but not necessarily
    // the same object reference) as the array that was passed to the constructor.
    public override fun encode(samples: LongArray, out: BitOutputStream) {
        writeTypeAndShift(1, out)
        for (`val`: Long in samples) writeRawSample(`val` shr sampleShift, out)
    }

    companion object {
        // Computes the best way to encode the given values under the verbatim coding mode,
        // returning an exact size plus a new encoder object associated with the input arguments.
        fun computeBest(samples: LongArray, shift: Int, depth: Int): SizeEstimate<SubframeEncoder?> {
            val enc: VerbatimEncoder = VerbatimEncoder(samples, shift, depth)
            val size: Long = (1 + 6 + 1 + shift + (samples.size * depth)).toLong()
            return SizeEstimate(size, enc)
        }
    }
}
*/
