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
package io.nayuki.flac.app

/*
import io.nayuki.flac.common.StreamInfo
import io.nayuki.flac.common.StreamInfo.md5Hash
import io.nayuki.flac.decode.DataFormatException
import io.nayuki.flac.decode.FlacDecoder
import kotlin.jvm.JvmStatic

/**
 * Decodes a FLAC file to an uncompressed PCM WAV file. Overwrites output file if already exists.
 * Runs silently if successful, otherwise prints error messages to standard error.
 *
 * Usage: java DecodeFlacToWav InFile.flac OutFile.wav
 *
 * Requirements on the FLAC file:
 *
 *  * Sample depth is 8, 16, 24, or 32 bits (not 4, 17, 23, etc.)
 *  * Contains no ID3v1 or ID3v2 tags, or other data unrecognized by the FLAC format
 *  * Correct total number of samples (not zero) is stored in stream info block
 *  * Every frame has a correct header, subframes do not overflow the sample depth,
 * and other strict checks enforced by this decoder library
 *
 */
object DecodeFlacToWav {
    @Throws(java.io.IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Handle command line arguments
        if (args.size != 2) {
            java.lang.System.err.println("Usage: java DecodeFlacToWav InFile.flac OutFile.wav")
            java.lang.System.exit(1)
            return
        }
        val inFile: java.io.File = java.io.File(args[0])
        val outFile: java.io.File = java.io.File(args[1])

        // Decode input FLAC file
        var streamInfo: StreamInfo
        var samples: Array<IntArray>
        FlacDecoder(inFile).use { dec ->

            // Handle metadata header blocks
            while (dec.readAndHandleMetadataBlock() != null);
            streamInfo = dec.streamInfo
            if (streamInfo.sampleDepth % 8 != 0) throw java.lang.UnsupportedOperationException("Only whole-byte sample depth supported")

            // Decode every block
            samples = Array(streamInfo.numChannels) { IntArray(streamInfo.numSamples.toInt()) }
            var off = 0
            while (true) {
                val len = dec.readAudioBlock(samples, off)
                if (len == 0) break
                off += len
            }
        }

        // Check audio MD5 hash
        val expectHash = streamInfo.md5Hash
        if (java.util.Arrays.equals(
                expectHash,
                ByteArray(16)
            )
        ) java.lang.System.err.println("Warning: MD5 hash field was blank") else if (!java.util.Arrays.equals(
                StreamInfo.md5Hash, expectHash
            )
        ) throw DataFormatException("MD5 hash check failed")
        // Else the hash check passed

        // Start writing WAV output file
        val bytesPerSample = streamInfo.sampleDepth / 8
        java.io.DataOutputStream(
            java.io.BufferedOutputStream(java.io.FileOutputStream(outFile))
        ).use { out ->
            DecodeFlacToWav.out = out

            // Header chunk
            val sampleDataLen = samples[0].size * streamInfo.numChannels * bytesPerSample
            out.writeInt(0x52494646) // "RIFF"
            writeLittleInt32(sampleDataLen + 36)
            out.writeInt(0x57415645) // "WAVE"

            // Metadata chunk
            out.writeInt(0x666D7420) // "fmt "
            writeLittleInt32(16)
            writeLittleInt16(0x0001)
            writeLittleInt16(streamInfo.numChannels)
            writeLittleInt32(streamInfo.sampleRate)
            writeLittleInt32(streamInfo.sampleRate * streamInfo.numChannels * bytesPerSample)
            writeLittleInt16(streamInfo.numChannels * bytesPerSample)
            writeLittleInt16(streamInfo.sampleDepth)

            // Audio data chunk ("data")
            out.writeInt(0x64617461) // "data"
            writeLittleInt32(sampleDataLen)
            for (i in samples[0].indices) {
                for (j in samples.indices) {
                    val `val` = samples[j][i]
                    if (bytesPerSample == 1) out.write(`val` + 128) // Convert to unsigned, as per WAV PCM conventions
                    else {  // 2 <= bytesPerSample <= 4
                        for (k in 0 until bytesPerSample) out.write(`val` ushr k * 8) // Little endian
                    }
                }
            }
        }
    }

    // Helper members for writing WAV files
    private var out: java.io.DataOutputStream? = null
    @Throws(java.io.IOException::class)
    private fun writeLittleInt16(x: Int) {
        out.writeShort(java.lang.Integer.reverseBytes(x) ushr 16)
    }

    @Throws(java.io.IOException::class)
    private fun writeLittleInt32(x: Int) {
        out.writeInt(java.lang.Integer.reverseBytes(x))
    }
}
*/
