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
 * Under the linear predictive coding (LPC) mode of some order, this provides size estimates on and bitstream encoding of audio sample data.
 */
internal class LinearPredictiveEncoder(samples: LongArray, shift: Int, depth: Int, order: Int, fdp: FastDotProduct) :
    SubframeEncoder(shift, depth) {
    private val order: Int
    private val realCoefs: DoubleArray
    private var coefficients: IntArray
    private val coefDepth: Int
    private val coefShift: Int
    var riceOrder: Int = 0

    init {
        val numSamples: Int = samples.size
        if ((order < 1) || (order > 32) || (numSamples < order)) throw IllegalArgumentException()
        this.order = order

        // Set up matrix to solve linear least squares problem
        val matrix: Array<DoubleArray> = Array(order, { DoubleArray(order + 1) })
        for (r in matrix.indices) {
            for (c in matrix.get(r).indices) {
                var `val`: Double
                if (c >= r) `val` = fdp.dotProduct(r, c, samples.size - order) else `val` = matrix.get(c).get(r)
                matrix.get(r).get(c) = `val`
            }
        }

        // Solve matrix, then examine range of coefficients
        realCoefs = solveMatrix(matrix)
        var maxCoef: Double = 0.0
        for (x: Double in realCoefs) maxCoef = Math.max(Math.abs(x), maxCoef)
        val wholeBits: Int = if (maxCoef >= 1) (Math.log(maxCoef) / Math.log(2.0)).toInt() + 1 else 0

        // Quantize and store the coefficients
        coefficients = IntArray(order)
        coefDepth = 15 // The maximum possible
        coefShift = coefDepth - 1 - wholeBits
        for (i in realCoefs.indices) {
            val coef: Double = realCoefs.get(realCoefs.size - 1 - i)
            val `val`: Int = Math.round(coef * (1 shl coefShift)).toInt()
            coefficients.get(i) = Math.max(Math.min(`val`, (1 shl (coefDepth - 1)) - 1), -(1 shl (coefDepth - 1)))
        }
    }

    @Throws(IOException::class)
    public override fun encode(samples: LongArray, out: BitOutputStream) {
        var samples: LongArray = samples
        Objects.requireNonNull(samples)
        Objects.requireNonNull(out)
        if (samples.size < order) throw IllegalArgumentException()
        writeTypeAndShift(32 + order - 1, out)
        samples = shiftRight(samples, sampleShift)
        for (i in 0 until order)  // Warmup
            writeRawSample(samples.get(i), out)
        out.writeInt(4, coefDepth - 1)
        out.writeInt(5, coefShift)
        for (x: Int in coefficients) out.writeInt(coefDepth, x)
        applyLpc(samples, coefficients, coefShift)
        RiceEncoder.encode(samples, order, riceOrder, out)
    }

    companion object {
        // Computes a good way to encode the given values under the linear predictive coding (LPC) mode of the given order,
        // returning a size plus a new encoder object associated with the input arguments. This process of minimizing the size
        // has an enormous search space, and it is impossible to guarantee the absolute optimal solution. The maxRiceOrder argument
        // is used by the Rice encoder to estimate the size of coding the residual signal. The roundVars argument controls
        // how many different coefficients are tested rounding both up and down, resulting in exponential time behavior.
        fun computeBest(
            samples: LongArray,
            shift: Int,
            depth: Int,
            order: Int,
            roundVars: Int,
            fdp: FastDotProduct,
            maxRiceOrder: Int
        ): SizeEstimate<SubframeEncoder?> {
            // Check arguments
            var samples: LongArray = samples
            if (order < 1 || order > 32) throw IllegalArgumentException()
            if ((roundVars < 0) || (roundVars > order) || (roundVars > 30)) throw IllegalArgumentException()
            val enc: LinearPredictiveEncoder = LinearPredictiveEncoder(samples, shift, depth, order, fdp)
            samples = shiftRight(samples, shift)
            val residues: DoubleArray?
            var indices: Array<Int>? = null
            val scaler: Int = 1 shl enc.coefShift
            if (roundVars > 0) {
                residues = DoubleArray(order)
                indices = arrayOfNulls(order)
                for (i in 0 until order) {
                    residues.get(i) =
                        Math.abs(Math.round(enc.realCoefs.get(i) * scaler) - enc.realCoefs.get(i) * scaler)
                    indices.get(i) = i
                }
                Arrays.sort(indices, object : Comparator<Int?> {
                    public override fun compare(x: Int?, y: Int?): Int {
                        return java.lang.Double.compare(residues.get((y)!!), residues.get((x)!!))
                    }
                })
            } else residues = null
            var bestSize: Long = Long.MAX_VALUE
            var bestCoefs: IntArray = enc.coefficients.clone()
            for (i in 0 until (1 shl roundVars)) {
                for (j in 0 until roundVars) {
                    val k: Int = indices!!.get(j)
                    val coef: Double = enc.realCoefs.get(k)
                    var `val`: Int
                    if (((i ushr j) and 1) == 0) `val` = Math.floor(coef * scaler).toInt() else `val` =
                        Math.ceil(coef * scaler).toInt()
                    enc.coefficients.get(order - 1 - k) =
                        Math.max(Math.min(`val`, (1 shl (enc.coefDepth - 1)) - 1), -(1 shl (enc.coefDepth - 1)))
                }
                val newData: LongArray = if (roundVars > 0) samples.clone() else samples
                applyLpc(newData, enc.coefficients, enc.coefShift)
                val temp: Long = RiceEncoder.computeBestSizeAndOrder(newData, order, maxRiceOrder)
                val size: Long = 1 + 6 + 1 + shift + (order * depth) + (temp ushr 4)
                if (size < bestSize) {
                    bestSize = size
                    bestCoefs = enc.coefficients.clone()
                    enc.riceOrder = (temp and 0xFL).toInt()
                }
            }
            enc.coefficients = bestCoefs
            return SizeEstimate(bestSize, enc)
        }

        // Solves an n * (n+1) augmented matrix (which modifies its values as a side effect),
        // returning a new solution vector of length n.
        private fun solveMatrix(mat: Array<DoubleArray>): DoubleArray {
            // Gauss-Jordan elimination algorithm
            val rows: Int = mat.size
            val cols: Int = mat.get(0).size
            if (rows + 1 != cols) throw IllegalArgumentException()

            // Forward elimination
            var numPivots: Int = 0
            var j: Int = 0
            while (j < rows && numPivots < rows) {
                var pivotRow: Int = rows
                var pivotMag: Double = 0.0
                for (i in numPivots until rows) {
                    if (Math.abs(mat.get(i).get(j)) > pivotMag) {
                        pivotMag = Math.abs(mat.get(i).get(j))
                        pivotRow = i
                    }
                }
                if (pivotRow == rows) {
                    j++
                    continue
                }
                val temp: DoubleArray = mat.get(numPivots)
                mat.get(numPivots) = mat.get(pivotRow)
                mat.get(pivotRow) = temp
                pivotRow = numPivots
                numPivots++
                var factor: Double = mat.get(pivotRow).get(j)
                for (k in 0 until cols) mat.get(pivotRow).get(k) /= factor
                mat.get(pivotRow).get(j) = 1.0
                for (i in pivotRow + 1 until rows) {
                    factor = mat.get(i).get(j)
                    for (k in 0 until cols) mat.get(i).get(k) -= mat.get(pivotRow).get(k) * factor
                    mat.get(i).get(j) = 0.0
                }
                j++
            }

            // Back substitution
            val result: DoubleArray = DoubleArray(rows)
            for (i in numPivots - 1 downTo 0) {
                var pivotCol: Int = 0
                while (pivotCol < cols && mat.get(i).get(pivotCol) == 0.0) pivotCol++
                if (pivotCol == cols) continue
                result.get(pivotCol) = mat.get(i).get(cols - 1)
                for (j in i - 1 downTo 0) {
                    val factor: Double = mat.get(j).get(pivotCol)
                    for (k in 0 until cols) mat.get(j).get(k) -= mat.get(i).get(k) * factor
                    mat.get(j).get(pivotCol) = 0.0
                }
            }
            return result
        }

        /*---- Static helper functions ----*/ // Applies linear prediction to data[coefs.length : data.length] so that newdata[i] =
        // data[i] - ((data[i-1]*coefs[0] + data[i-2]*coefs[1] + ... + data[i-coefs.length]*coefs[coefs.length]) >> shift).
        // By FLAC parameters, each data[i] must fit in a signed 33-bit integer, each coef must fit in signed int15, and coefs.length <= 32.
        // When these preconditions are met, they guarantee the lack of arithmetic overflow in the computation and results,
        // and each value written back to the data array fits in a signed int53.
        fun applyLpc(data: LongArray, coefs: IntArray, shift: Int) {
            // Check arguments and arrays strictly
            Objects.requireNonNull(data)
            Objects.requireNonNull(coefs)
            if ((coefs.size > 32) || (shift < 0) || (shift > 63)) throw IllegalArgumentException()
            for (x: Long in data) {
                x = x shr 32
                if (x != 0L && x != -1L) // Check if it fits in signed int33
                    throw IllegalArgumentException()
            }
            for (x: Int in coefs) {
                x = x shr 14
                if (x != 0 && x != -1) // Check if it fits in signed int15
                    throw IllegalArgumentException()
            }

            // Perform the LPC convolution/FIR
            for (i in data.size - 1 downTo coefs.size) {
                var sum: Long = 0
                for (j in coefs.indices) sum += data.get(i - 1 - j) * coefs.get(j)
                val `val`: Long = data.get(i) - (sum shr shift)
                if ((`val` shr 52) != 0L && (`val` shr 52) != -1L) // Check if it fits in signed int53
                    throw AssertionError()
                data.get(i) = `val`
            }
        }

        // Returns a new array where each result[i] = data[i] >> shift.
        fun shiftRight(data: LongArray, shift: Int): LongArray {
            Objects.requireNonNull(data)
            if (shift < 0 || shift > 63) throw IllegalArgumentException()
            val result: LongArray = LongArray(data.size)
            for (i in data.indices) result.get(i) = data.get(i) shr shift
            return result
        }
    }
}
*/
