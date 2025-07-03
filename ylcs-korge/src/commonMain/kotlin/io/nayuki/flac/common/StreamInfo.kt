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

import io.nayuki.flac.decode.*
import korlibs.io.lang.*

/**
 * Represents precisely all the fields of a stream info metadata block. Mutable structure,
 * not thread-safe. Also has methods for parsing and serializing this structure to/from bytes.
 * All fields can be modified freely when no method call is active.
 * @see FrameInfo
 *
 * @see FlacDecoder
 */
class StreamInfo {
    /*---- Fields about block and frame sizes ----*/
    /**
     * Minimum block size (in samples per channel) among the whole stream, a uint16 value.
     * However when minBlockSize = maxBlockSize (constant block size encoding style),
     * the final block is allowed to be smaller than minBlockSize.
     */
    var minBlockSize = 0

    /**
     * Maximum block size (in samples per channel) among the whole stream, a uint16 value.
     */
    var maxBlockSize = 0

    /**
     * Minimum frame size (in bytes) among the whole stream, a uint24 value.
     * However, a value of 0 signifies that the value is unknown.
     */
    var minFrameSize = 0

    /**
     * Maximum frame size (in bytes) among the whole stream, a uint24 value.
     * However, a value of 0 signifies that the value is unknown.
     */
    var maxFrameSize = 0
    /*---- Fields about stream properties ----*/
    /**
     * The sample rate of the audio stream (in hertz (Hz)), a positive uint20 value.
     * Note that 0 is an invalid value.
     */
    var sampleRate = 0

    /**
     * The number of channels in the audio stream, between 1 and 8 inclusive.
     * 1 means mono, 2 means stereo, et cetera.
     */
    var numChannels = 0

    /**
     * The bits per sample in the audio stream, in the range 4 to 32 inclusive.
     */
    var sampleDepth = 0

    /**
     * The total number of samples per channel in the whole stream, a uint36 value.
     * The special value of 0 signifies that the value is unknown (not empty zero-length stream).
     */
    var numSamples: Long = 0

    /**
     * The 16-byte MD5 hash of the raw uncompressed audio data serialized in little endian with
     * channel interleaving (not planar). It can be all zeros to signify that the hash was not computed.
     * It is okay to replace this array as needed (the initially constructed array object is not special).
     */
    var md5Hash: ByteArray
    /*---- Constructors ----*/
    /**
     * Constructs a blank stream info structure with certain default values.
     */
    constructor() {
        // Set these fields to legal unknown values
        minFrameSize = 0
        maxFrameSize = 0
        numSamples = 0
        md5Hash = ByteArray(16)

        // Set these fields to invalid (not reserved) values
        minBlockSize = 0
        maxBlockSize = 0
        sampleRate = 0
    }

    /**
     * Constructs a stream info structure by parsing the specified 34-byte metadata block.
     * (The array must contain only the metadata payload, without the type or length fields.)
     * @param b the metadata block's payload data to parse (not `null`)
     * @throws NullPointerException if the array is `null`
     * @throws IllegalArgumentException if the array length is not 34
     * @throws DataFormatException if the data contains invalid values
     */
    constructor(b: ByteArray) {
        require(b.size == 34) { "Invalid data length" }
        try {
            val `in`: FlacLowLevelInput = ByteArrayFlacInput(b)
            minBlockSize = `in`.readUint(16)
            maxBlockSize = `in`.readUint(16)
            minFrameSize = `in`.readUint(24)
            maxFrameSize = `in`.readUint(24)
            if (minBlockSize < 16) throw DataFormatException("Minimum block size less than 16")
            if (maxBlockSize > 65535) throw DataFormatException("Maximum block size greater than 65535")
            if (maxBlockSize < minBlockSize) throw DataFormatException("Maximum block size less than minimum block size")
            if (minFrameSize != 0 && maxFrameSize != 0 && maxFrameSize < minFrameSize) throw DataFormatException("Maximum frame size less than minimum frame size")
            sampleRate = `in`.readUint(20)
            if (sampleRate == 0 || sampleRate > 655350) throw DataFormatException("Invalid sample rate")
            numChannels = `in`.readUint(3) + 1
            sampleDepth = `in`.readUint(5) + 1
            numSamples = `in`.readUint(18).toLong() shl 18 or `in`.readUint(18).toLong() // uint36
            md5Hash = ByteArray(16)
            `in`.readFully(md5Hash)
            // Skip closing the in-memory stream
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }
    /*---- Methods ----*/
    /**
     * Checks the state of this object, and either returns silently or throws an exception.
     * @throws NullPointerException if the MD5 hash array is `null`
     * @throws IllegalStateException if any field has an invalid value
     */
    fun checkValues() {
        check(minBlockSize ushr 16 == 0) { "Invalid minimum block size" }
        check(maxBlockSize ushr 16 == 0) { "Invalid maximum block size" }
        check(minFrameSize ushr 24 == 0) { "Invalid minimum frame size" }
        check(maxFrameSize ushr 24 == 0) { "Invalid maximum frame size" }
        check(!(sampleRate == 0 || sampleRate ushr 20 != 0)) { "Invalid sample rate" }
        check(!(numChannels < 1 || numChannels > 8)) { "Invalid number of channels" }
        check(!(sampleDepth < 4 || sampleDepth > 32)) { "Invalid sample depth" }
        check(numSamples ushr 36 == 0L) { "Invalid number of samples" }
        check(md5Hash.size == 16) { "Invalid MD5 hash length" }
    }

    /**
     * Checks whether the specified frame information is consistent with values in
     * this stream info object, either returning silently or throwing an exception.
     * @param meta the frame info object to check (not `null`)
     * @throws NullPointerException if the frame info is `null`
     * @throws DataFormatException if the frame info contains bad values
     */
    fun checkFrame(meta: FrameInfo) {
        if (meta.numChannels != numChannels) throw DataFormatException("Channel count mismatch")
        if (meta.sampleRate != -1 && meta.sampleRate != sampleRate) throw DataFormatException("Sample rate mismatch")
        if (meta.sampleDepth != -1 && meta.sampleDepth != sampleDepth) throw DataFormatException("Sample depth mismatch")
        if (numSamples != 0L && meta.blockSize > numSamples) throw DataFormatException("Block size exceeds total number of samples")
        if (meta.blockSize > maxBlockSize) throw DataFormatException("Block size exceeds maximum")
        // Note: If minBlockSize == maxBlockSize, then the final block
        // in the stream is allowed to be smaller than minBlockSize
        if (minFrameSize != 0 && meta.frameSize < minFrameSize) throw DataFormatException("Frame size less than minimum")
        if (maxFrameSize != 0 && meta.frameSize > maxFrameSize) throw DataFormatException("Frame size exceeds maximum")
    }

    ///**
    // * Writes this stream info metadata block to the specified output stream, including the
    // * metadata block header, writing exactly 38 bytes. (This is unlike the constructor,
    // * which takes an array without the type and length fields.) The output stream must
    // * initially be aligned to a byte boundary, and will finish at a byte boundary.
    // * @param last whether the metadata block is the final one in the FLAC file
    // * @param out the output stream to write to (not `null`)
    // * @throws NullPointerException if the output stream is `null`
    // * @throws IOException if an I/O exception occurred
    // */
    //@Throws(IOException::class)
    //fun write(last: Boolean, out: BitOutputStream) {
    //    // Check arguments and state
    //    checkValues()
    //    // Write metadata block header
    //    out.writeInt(1, if (last) 1 else 0)
    //    out.writeInt(7, 0) // Type
    //    out.writeInt(24, 34) // Length
    //    // Write stream info block fields
    //    out.writeInt(16, minBlockSize)
    //    out.writeInt(16, maxBlockSize)
    //    out.writeInt(24, minFrameSize)
    //    out.writeInt(24, maxFrameSize)
    //    out.writeInt(20, sampleRate)
    //    out.writeInt(3, numChannels - 1)
    //    out.writeInt(5, sampleDepth - 1)
    //    out.writeInt(18, (numSamples ushr 18).toInt())
    //    out.writeInt(18, (numSamples ushr 0).toInt())
    //    for (b in md5Hash) out.writeInt(8, b.toInt())
    //}
}
