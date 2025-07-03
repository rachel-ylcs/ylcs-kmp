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
import java.util.*

/* 
 * Speeds up computations of a signal vector's autocorrelation by avoiding redundant
 * arithmetic operations. Acts as a helper class for LinearPredictiveEncoder.
 * Objects of this class are intended to be immutable, but can't enforce it because
 * they store a reference to a caller-controlled array without making a private copy.
 */
internal class FastDotProduct(data: LongArray, maxDelta: Int) {
    /*---- Fields ----*/ // Not null, and precomputed.length <= data.length.
    private val data: LongArray

    // precomputed[i] = dotProduct(0, i, data.length - i). In other words, it is the sum
    // the of products of all unordered pairs of elements whose indices differ by i.
    private val precomputed: DoubleArray

    /*---- Constructors ----*/ // Constructs a fast dot product calculator over the given array, with the given maximum difference in indexes.
    // This pre-calculates some dot products and caches them so that future queries can be answered faster.
    // To avoid the cost of copying the entire vector, a reference to the array is saved into this object.
    // The values from data array are still needed when dotProduct() is called, thus no other code is allowed to modify the values.
    init {
        // Check arguments
        this.data = Objects.requireNonNull(data)
        if (maxDelta < 0 || maxDelta >= data.size) throw IllegalArgumentException()

        // Precompute some dot products
        precomputed = DoubleArray(maxDelta + 1)
        for (i in precomputed.indices) {
            var sum: Double = 0.0
            var j: Int = 0
            while (i + j < data.size) {
                sum += data.get(j).toDouble() * data.get(i + j)
                j++
            }
            precomputed.get(i) = sum
        }
    }

    /*---- Methods ----*/ // Returns the dot product of data[off0 : off0 + len] with data[off1 : off1 + len],
    // i.e. data[off0]*data[off1] + data[off0+1]*data[off1+1] + ... + data[off0+len-1]*data[off1+len-1],
    // with potential rounding error. Note that all the endpoints must lie within the bounds
    // of the data array. Also, this method requires abs(off0 - off1) <= maxDelta.
    fun dotProduct(off0: Int, off1: Int, len: Int): Double {
        if (off0 > off1) // Symmetric case
            return dotProduct(off1, off0, len)

        // Check arguments
        if ((off0 < 0) || (off1 < 0) || (len < 0) || (data.size - len < off1)) throw IndexOutOfBoundsException()
        assert(off0 <= off1)
        val delta: Int = off1 - off0
        if (delta > precomputed.size) throw IllegalArgumentException()

        // Add up a small number of products to remove from the precomputed sum
        var removal: Double = 0.0
        for (i in 0 until off0) removal += data.get(i).toDouble() * data.get(i + delta)
        for (i in off1 + len until data.size) removal += data.get(i).toDouble() * data.get(i - delta)
        return precomputed.get(delta) - removal
    }
}
*/
