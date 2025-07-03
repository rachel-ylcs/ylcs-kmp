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
import java.util.*

/* 
 * Under the fixed prediction coding mode of some order, this provides size calculations on and bitstream encoding of audio sample data.
 */
internal class FixedPredictionEncoder(samples: LongArray, shift: Int, depth: Int, order: Int) :
    SubframeEncoder(shift, depth) {
    private val order: Int
    var riceOrder: Int = 0
    @Throws(IOException::class)
    public override fun encode(samples: LongArray, out: BitOutputStream) {
        var samples: LongArray = samples
        Objects.requireNonNull(samples)
        Objects.requireNonNull(out)
        if (samples.size < order) throw IllegalArgumentException()
        writeTypeAndShift(8 + order, out)
        samples = LinearPredictiveEncoder.Companion.shiftRight(samples, sampleShift)
        for (i in 0 until order)  // Warmup
            writeRawSample(samples.get(i), out)
        LinearPredictiveEncoder.Companion.applyLpc(samples, COEFFICIENTS.get(order), 0)
        RiceEncoder.encode(samples, order, riceOrder, out)
    }

    init {
        if ((order < 0) || (order >= COEFFICIENTS.size) || (samples.size < order)) throw IllegalArgumentException()
        this.order = order
    }

    companion object {
        // Computes the best way to encode the given values under the fixed prediction coding mode of the given order,
        // returning a size plus a new encoder object associated with the input arguments. The maxRiceOrder argument
        // is used by the Rice encoder to estimate the size of coding the residual signal.
        fun computeBest(
            samples: LongArray,
            shift: Int,
            depth: Int,
            order: Int,
            maxRiceOrder: Int
        ): SizeEstimate<SubframeEncoder?> {
            var samples: LongArray = samples
            val enc: FixedPredictionEncoder = FixedPredictionEncoder(samples, shift, depth, order)
            samples = LinearPredictiveEncoder.Companion.shiftRight(samples, shift)
            LinearPredictiveEncoder.Companion.applyLpc(samples, COEFFICIENTS.get(order), 0)
            val temp: Long = RiceEncoder.computeBestSizeAndOrder(samples, order, maxRiceOrder)
            enc.riceOrder = (temp and 0xFL).toInt()
            val size: Long = 1 + 6 + 1 + shift + (order * depth) + (temp ushr 4)
            return SizeEstimate(size, enc)
        }

        // The linear predictive coding (LPC) coefficients for fixed prediction of orders 0 to 4 (inclusive).
        private val COEFFICIENTS: Array<IntArray> =
            arrayOf(intArrayOf(), intArrayOf(1), intArrayOf(2, -1), intArrayOf(3, -3, 1), intArrayOf(4, -6, 4, -1))
    }
}
*/
