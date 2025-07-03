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
import korlibs.io.lang.*

/* 
 * Calculates/estimates the encoded size of a vector of residuals, and also performs the encoding to an output stream.
 */
internal object RiceEncoder {
    /*---- Functions for size calculation ---*/ // Calculates the best number of bits and partition order needed to encode the values data[warmup : data.length].
    // Each value in that subrange of data must fit in a signed 53-bit integer. The result is packed in the form
    // ((bestSize << 4) | bestOrder), where bestSize is an unsigned integer and bestOrder is a uint4.
    // Note that the partition orders searched, and hence the resulting bestOrder, are in the range [0, maxPartOrder].
    fun computeBestSizeAndOrder(data: LongArray, warmup: Int, maxPartOrder: Int): Long {
        // Check arguments strictly
        require(!(warmup < 0 || warmup > data.size))
        require(!(maxPartOrder < 0 || maxPartOrder > 15))
        for (x in data) {
            var x = x shr 52
            require(
                !(x != 0L && x != -1L) // Check that it fits in a signed int53
            )
        }
        var bestSize = Int.MAX_VALUE.toLong()
        var bestOrder = -1
        var escapeBits: IntArray? = null
        var bitsAtParam: IntArray? = null
        for (order in maxPartOrder downTo 0) {
            val partSize = data.size ushr order
            if (partSize shl order != data.size || partSize < warmup) continue
            val numPartitions = 1 shl order
            if (escapeBits == null) {  // And bitsAtParam == null
                escapeBits = IntArray(numPartitions)
                bitsAtParam = IntArray(numPartitions * 16)
                for (i in warmup until data.size) {
                    val j = i / partSize
                    var `val` = data[i]
                    escapeBits[j] =
                        kotlin.math.max(65 - (`val` xor (`val` shr 63)).countLeadingZeroBits(), escapeBits.get(j))
                    `val` = if (`val` >= 0) `val` shl 1 else (-`val` shl 1) - 1
                    var param = 0
                    while (param < 15) {
                        bitsAtParam[param + j * 16] += (`val` + 1 + param).toInt()
                        param++
                        `val` = `val` ushr 1
                    }
                }
            } else {  // Both arrays are non-null
                // Logically halve the size of both arrays (but without reallocating to the true new size)
                for (i in 0 until numPartitions) {
                    val j = i shl 1
                    escapeBits[i] = kotlin.math.max(escapeBits[j], escapeBits[j + 1])
                    for (param in 0..14) bitsAtParam!![param + i * 16] =
                        bitsAtParam[param + j * 16] + bitsAtParam[param + (j + 1) * 16]
                }
            }
            var size = (4 + (4 shl order)).toLong()
            for (i in 0 until numPartitions) {
                var min = Int.MAX_VALUE
                if (escapeBits.get(i) <= 31) min = 5 + escapeBits.get(i) * (partSize - if (i == 0) warmup else 0)
                for (param in 0..14) min = kotlin.math.min(bitsAtParam!![param + i * 16], min)
                size += min.toLong()
            }
            if (size < bestSize) {
                bestSize = size
                bestOrder = order
            }
        }
        if (bestSize.toInt() == Int.MAX_VALUE || bestOrder ushr 4 != 0) throw AssertionError()
        return bestSize shl 4 or bestOrder.toLong()
    }

    // Calculates the number of bits needed to encode the sequence of values
    // data[start : end] with an optimally chosen Rice parameter.
    private fun computeBestSizeAndParam(data: LongArray?, start: Int, end: Int): Long {
        assert(data != null && 0 <= start && start <= end && end <= data.size)

        // Use escape code
        var bestParam: Int
        var bestSize: Long
        run {
            var accumulator: Long = 0
            for (i in start until end) {
                val `val`: Long = data!!.get(i)
                accumulator = accumulator or (`val` xor (`val` shr 63))
            }
            val numBits: Int = 65 - accumulator.countLeadingZeroBits()
            assert(numBits in 1..65)
            if (numBits <= 31) {
                bestSize = (4 + 5 + ((end - start) * numBits)).toLong()
                bestParam = 16 + numBits
                if ((bestParam ushr 6) != 0) throw AssertionError()
            } else {
                bestSize = Long.MAX_VALUE
                bestParam = 0
            }
        }

        // Use Rice coding
        for (param in 0..14) {
            var size: Long = 4
            for (i in start until end) {
                var `val` = data!![i]
                `val` = if (`val` >= 0) `val` shl 1 else (-`val` shl 1) - 1
                size += (`val` ushr param) + 1 + param
            }
            if (size < bestSize) {
                bestSize = size
                bestParam = param
            }
        }
        return bestSize shl 6 or bestParam.toLong()
    }

    /*---- Functions for encoding data ---*/ // Encodes the sequence of values data[warmup : data.length] with an appropriately chosen order and Rice parameters.
    // Each value in data must fit in a signed 53-bit integer.
    @Throws(IOException::class)
    fun encode(data: LongArray, warmup: Int, order: Int, out: BitOutputStream) {
        // Check arguments strictly
        if (warmup < 0 || warmup > data.size) throw IllegalArgumentException()
        if (order < 0 || order > 15) throw IllegalArgumentException()
        for (x in data) {
            val x = x shr 52
            if (x != 0L && x != -1L) // Check that it fits in a signed int53
                throw IllegalArgumentException()
        }
        out.writeInt(2, 0)
        out.writeInt(4, order)
        val numPartitions = 1 shl order
        var start = warmup
        var end = data.size ushr order
        for (i in 0 until numPartitions) {
            val param = computeBestSizeAndParam(data, start, end).toInt() and 0x3F
            encode(data, start, end, param, out)
            start = end
            end += data.size ushr order
        }
    }

    // Encodes the sequence of values data[start : end] with the given Rice parameter.
    @Throws(IOException::class)
    private fun encode(data: LongArray?, start: Int, end: Int, param: Int, out: BitOutputStream?) {
        assert(0 <= param && param <= 31 && data != null && out != null)
        assert(0 <= start && start <= end && end <= data!!.size)
        if (param < 15) {
            out!!.writeInt(4, param)
            for (j in start until end) writeRiceSignedInt(data!![j], param, out)
        } else {
            out!!.writeInt(4, 15)
            val numBits = param - 16
            out.writeInt(5, numBits)
            for (j in start until end) out.writeInt(numBits, data!![j].toInt())
        }
    }

    @Throws(IOException::class)
    private fun writeRiceSignedInt(`val`: Long, param: Int, out: BitOutputStream?) {
        assert(0 <= param && param <= 31 && out != null)
        assert(
            `val` shr 52 == 0L || `val` shr 52 == -1L // Fits in a signed int53
        )
        val unsigned = if (`val` >= 0) `val` shl 1 else (-`val` shl 1) - 1
        val unary = (unsigned ushr param).toInt()
        for (i in 0 until unary) out!!.writeInt(1, 0)
        out!!.writeInt(1, 1)
        out.writeInt(param, unsigned.toInt())
    }
}
*/
