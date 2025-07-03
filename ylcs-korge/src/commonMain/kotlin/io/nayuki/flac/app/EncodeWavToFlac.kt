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
import io.nayuki.flac.decode.DataFormatException
import io.nayuki.flac.encode.BitOutputStream
import kotlin.jvm.JvmStatic

/**
 * Encodes an uncompressed PCM WAV file to a FLAC file.
 * Overwrites the output file if it already exists.
 *
 * Usage: java EncodeWavToFlac InFile.wav OutFile.flac
 *
 * Requirements on the WAV file:
 *
 *  * Sample depth is 8, 16, 24, or 32 bits (not 4, 17, 23, etc.)
 *  * Number of channels is between 1 to 8 inclusive
 *  * Sample rate is less than 2<sup>20</sup> hertz
 *
 */
object EncodeWavToFlac {
    @Throws(java.io.IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Handle command line arguments
        if (args.size != 2) {
            java.lang.System.err.println("Usage: java EncodeWavToFlac InFile.wav OutFile.flac")
            java.lang.System.exit(1)
            return
        }
        val inFile: java.io.File = java.io.File(args[0])
        val outFile: java.io.File = java.io.File(args[1])

        // Read WAV file headers and audio sample data
        var samples: Array<IntArray>
        var sampleRate: Int
        var sampleDepth: Int
        java.io.BufferedInputStream(java.io.FileInputStream(inFile)).use { `in` ->
            // Parse and check WAV header
            if (readString(`in`, 4) != "RIFF") throw DataFormatException("Invalid RIFF file header")
            readLittleUint(`in`, 4) // Remaining data length
            if (readString(`in`, 4) != "WAVE") throw DataFormatException("Invalid WAV file header")

            // Handle the format chunk
            if (readString(`in`, 4) != "fmt ") throw DataFormatException("Unrecognized WAV file chunk")
            if (readLittleUint(`in`, 4) != 16) throw DataFormatException("Unsupported WAV file type")
            if (readLittleUint(`in`, 2) != 0x0001) throw DataFormatException("Unsupported WAV file codec")
            val numChannels = readLittleUint(`in`, 2)
            if (numChannels < 0 || numChannels > 8) throw java.lang.RuntimeException("Too many (or few) audio channels")
            sampleRate = readLittleUint(`in`, 4)
            if (sampleRate <= 0 || sampleRate >= 1 shl 20) throw java.lang.RuntimeException("Sample rate too large or invalid")
            val byteRate = readLittleUint(`in`, 4)
            val blockAlign = readLittleUint(`in`, 2)
            sampleDepth = readLittleUint(`in`, 2)
            if (sampleDepth == 0 || sampleDepth > 32 || sampleDepth % 8 != 0) throw java.lang.RuntimeException("Unsupported sample depth")
            val bytesPerSample = sampleDepth / 8
            if (bytesPerSample * numChannels != blockAlign) throw java.lang.RuntimeException("Invalid block align value")
            if (bytesPerSample * numChannels * sampleRate != byteRate) throw java.lang.RuntimeException("Invalid byte rate value")

            // Handle the data chunk
            if (readString(`in`, 4) != "data") throw DataFormatException("Unrecognized WAV file chunk")
            val sampleDataLen = readLittleUint(`in`, 4)
            if (sampleDataLen <= 0 || sampleDataLen % (numChannels * bytesPerSample) != 0) throw DataFormatException("Invalid length of audio sample data")
            val numSamples = sampleDataLen / (numChannels * bytesPerSample)
            samples = Array(numChannels) { IntArray(numSamples) }
            for (i in 0 until numSamples) {
                for (ch in 0 until numChannels) {
                    var `val` = readLittleUint(`in`, bytesPerSample)
                    if (sampleDepth == 8) `val` -= 128 else `val` = `val` shl 32 - sampleDepth shr 32 - sampleDepth
                    samples[ch][i] = `val`
                }
            }
        }
        java.io.RandomAccessFile(outFile, "rw").use { raf ->
            raf.setLength(0) // Truncate an existing file
            val out = BitOutputStream(
                java.io.BufferedOutputStream(RandomAccessFileOutputStream(raf))
            )
            out.writeInt(32, 0x664C6143)

            // Populate and write the stream info structure
            val info = StreamInfo()
            info.sampleRate = sampleRate
            info.numChannels = samples.size
            info.sampleDepth = sampleDepth
            info.numSamples = samples[0].size.toLong()
            info.md5Hash = StreamInfo.md5Hash
            info.write(true, out)

            // Encode all frames
            FlacEncoder(info, samples, 4096, SubframeEncoder.SearchOptions.SUBSET_BEST, out)
            out.flush()

            // Rewrite the stream info metadata block, which is
            // located at a fixed offset in the file by definition
            raf.seek(4)
            info.write(true, out)
            out.flush()
        }
    }

    // Reads len bytes from the given stream and interprets them as a UTF-8 string.
    @Throws(java.io.IOException::class)
    private fun readString(`in`: java.io.InputStream, len: Int): String {
        val temp = ByteArray(len)
        for (i in temp.indices) {
            val b: Int = `in`.read()
            if (b == -1) throw java.io.EOFException()
            temp[i] = b.toByte()
        }
        return String(temp, java.nio.charset.StandardCharsets.UTF_8)
    }

    // Reads n bytes (0 <= n <= 4) from the given stream, interpreting
    // them as an unsigned integer encoded in little endian.
    @Throws(java.io.IOException::class)
    private fun readLittleUint(`in`: java.io.InputStream, n: Int): Int {
        var result = 0
        for (i in 0 until n) {
            val b: Int = `in`.read()
            if (b == -1) throw java.io.EOFException()
            result = result or (b shl i * 8)
        }
        return result
    }
}
*/
