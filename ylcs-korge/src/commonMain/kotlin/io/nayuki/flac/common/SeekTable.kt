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
package io.nayuki.flac.common

import korlibs.io.lang.*
import korlibs.io.stream.*

/**
 * Represents precisely all the fields of a seek table metadata block. Mutable structure,
 * not thread-safe. Also has methods for parsing and serializing this structure to/from bytes.
 * All fields and objects can be modified freely when no method call is active.
 * @see FlacDecoder
 */
class SeekTable() {
    /*---- Fields ----*/
    /**
     * The list of seek points in this seek table. It is okay to replace this
     * list as needed (the initially constructed list object is not special).
     */
    var points: MutableList<SeekPoint> = ArrayList()
    /*---- Constructors ----*/

    /**
     * Constructs a seek table by parsing the given byte array representing the metadata block.
     * (The array must contain only the metadata payload, without the type or length fields.)
     *
     * This constructor does not check the validity of the seek points, namely the ordering
     * of seek point offsets, so calling {@link#checkValues()} on the freshly constructed object
     * can fail. However, this does guarantee that every point's frameSamples field is a uint16.
     * @param b the metadata block's payload data to parse (not `null`)
     * @throws NullPointerException if the array is `null`
     * @throws IllegalArgumentException if the array length
     * is not a multiple of 18 (size of each seek point)
     */
    constructor(b: ByteArray) : this() {
        require(b.size % 18 == 0) { "Data contains a partial seek point" }
        try {
            val `in` = b.openFastStream()
            var i = 0
            while (i < b.size) {
                val p = SeekPoint()
                p.sampleOffset = `in`.readLong()
                p.fileOffset = `in`.readLong()
                p.frameSamples = `in`.readUnsignedShort()
                points.add(p)
                i += 18
            }
            // Skip closing the in-memory streams
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }
    /*---- Methods ----*/
    /**
     * Checks the state of this object and returns silently if all these criteria pass:
     *
     *  * No object is `null`
     *  * The frameSamples field of each point is a uint16 value
     *  * All points with sampleOffset = 1 (i.e. 0xFFF...FFF) are at the end of the list
     *  * All points with sampleOffset  1 have strictly increasing
     * values of sampleOffset and non-decreasing values of fileOffset
     *
     * @throws NullPointerException if the list or an element is `null`
     * @throws IllegalStateException if the current list of seek points is contains invalid data
     */
    fun checkValues() {
        // Check list and each point
        for (p in points) {
            check(p.frameSamples and 0xFFFF == p.frameSamples) { "Frame samples outside uint16 range" }
        }

        // Check ordering of points
        for (i in 1 until points.size) {
            val p = points[i]
            if (p.sampleOffset != -1L) {
                val q = points[i - 1]
                check(p.sampleOffset > q.sampleOffset) { "Sample offsets out of order" }
                check(p.fileOffset >= q.fileOffset) { "File offsets out of order" }
            }
        }
    }

    ///**
    // * Writes all the points of this seek table as a metadata block to the specified output stream,
    // * also indicating whether it is the last metadata block. (This does write the type and length
    // * fields for the metadata block, unlike the constructor which takes an array without those fields.)
    // * @param last whether the metadata block is the final one in the FLAC file
    // * @param out the output stream to write to (not `null`)
    // * @throws NullPointerException if the output stream is `null`
    // * @throws IllegalStateException if there are too many
    // * @throws IOException if an I/O exception occurred
    // * seek points (> 932067) or {@link#checkValues()} fails
    // */
    //@Throws(IOException::class)
    //fun write(last: Boolean, out: BitOutputStream) {
    //    // Check arguments and state
    //    check(points.size <= ((1 shl 24) - 1) / 18) { "Too many seek points" }
    //    checkValues()
    //    // Write metadata block header
    //    out.writeInt(1, if (last) 1 else 0)
    //    out.writeInt(7, 3)
    //    out.writeInt(24, points.size * 18)
    //    // Write each seek point
    //    for (p in points) {
    //        out.writeInt(32, (p.sampleOffset ushr 32).toInt())
    //        out.writeInt(32, (p.sampleOffset ushr 0).toInt())
    //        out.writeInt(32, (p.fileOffset ushr 32).toInt())
    //        out.writeInt(32, (p.fileOffset ushr 0).toInt())
    //        out.writeInt(16, p.frameSamples)
    //    }
    //}
    /*---- Helper structure ----*/
    /**
     * Represents a seek point entry in a seek table. Mutable structure, not thread-safe.
     * This class itself does not check the correctness of data, but other classes might.
     *
     * A seek point with data (sampleOffset = x, fileOffset = y, frameSamples = z) means
     * that at byte position (y + (byte offset of foremost audio frame)) in the file,
     * a FLAC frame begins (with the sync sequence), that frame has sample offset x
     * (where sample 0 is defined as the start of the audio stream),
     * and the frame contains z samples per channel.
     * @see SeekTable
     */
    class SeekPoint {
        /**
         * The sample offset in the audio stream, a uint64 value.
         * A value of -1 (i.e. 0xFFF...FFF) means this is a placeholder point.
         */
        var sampleOffset: Long = 0

        /**
         * The byte offset relative to the start of the foremost frame, a uint64 value.
         * If sampleOffset is -1, then this value is ignored.
         */
        var fileOffset: Long = 0

        /**
         * The number of audio samples in the target block/frame, a uint16 value.
         * If sampleOffset is -1, then this value is ignored.
         */
        var frameSamples = 0
    }
}
