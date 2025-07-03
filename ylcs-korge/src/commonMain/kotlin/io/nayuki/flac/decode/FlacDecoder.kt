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
package io.nayuki.flac.decode

import io.nayuki.flac.common.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.memory.*

/**
 * Handles high-level decoding and seeking in FLAC files. Also returns metadata blocks.
 * Every object is stateful, not thread-safe, and needs to be closed. Sample usage:
 * <pre>// Create a decoder
 * FlacDecoder dec = new FlacDecoder(...);
 *
 * &#x2F;/ Make the decoder process all metadata blocks internally.
 * &#x2F;/ We could capture the returned data for extra processing.
 * &#x2F;/ We must read all metadata before reading audio data.
 * while (dec.readAndHandleMetadataBlock() != null);
 *
 * &#x2F;/ Read audio samples starting from beginning
 * int[][] samples = (...);
 * dec.readAudioBlock(samples, ...);
 * dec.readAudioBlock(samples, ...);
 * dec.readAudioBlock(samples, ...);
 *
 * &#x2F;/ Seek to some position and continue reading
 * dec.seekAndReadAudioBlock(..., samples, ...);
 * dec.readAudioBlock(samples, ...);
 * dec.readAudioBlock(samples, ...);
 *
 * &#x2F;/ Close underlying file stream
 * dec.close();</pre>
 * @see FrameDecoder
 * @see FlacLowLevelInput
 */
class FlacDecoder(file: SyncStream) : AutoCloseable {
    /*---- Fields ----*/
    var streamInfo: StreamInfo? = null
    var seekTable: SeekTable? = null
    private var input: FlacLowLevelInput = SeekableFileFlacInput(file)
    private var metadataEndPos: Long
    private var frameDec: FrameDecoder? = null

    /*---- Constructors ----*/ // Constructs a new FLAC decoder to read the given file.
    // This immediately reads the basic header but not metadata blocks.
    init {
        // Initialize streams
        // Read basic header
        if (input.readUint(32) != 0x664C6143) // Magic string "fLaC"
            throw DataFormatException("Invalid magic string")
        metadataEndPos = -1
    }

    /*---- Methods ----*/ // Reads, handles, and returns the next metadata block. Returns a pair (Integer type, byte[] data) if the
    // next metadata block exists, otherwise returns null if the final metadata block was previously read.
    // In addition to reading and returning data, this method also updates the internal state
    // of this object to reflect the new data seen, and throws exceptions for situations such as
    // not starting with a stream info metadata block or encountering duplicates of certain blocks.
    fun readAndHandleMetadataBlock(): Array<Any>? {
        if (metadataEndPos != -1L) return null // All metadata already consumed

        // Read entire block
        val last = input!!.readUint(1) != 0
        val type = input!!.readUint(7)
        val length = input!!.readUint(24)
        val data = ByteArray(length)
        input!!.readFully(data)

        // Handle recognized block
        if (type == 0) {
            if (streamInfo != null) throw DataFormatException("Duplicate stream info metadata block")
            streamInfo = StreamInfo(data)
        } else {
            if (streamInfo == null) throw DataFormatException("Expected stream info metadata block")
            if (type == 3) {
                if (seekTable != null) throw DataFormatException("Duplicate seek table metadata block")
                seekTable = SeekTable(data)
            }
        }
        if (last) {
            metadataEndPos = input!!.position
            frameDec = FrameDecoder(input, streamInfo!!.sampleDepth)
        }
        return arrayOf(type, data)
    }

    // Reads and decodes the next block of audio samples into the given buffer,
    // returning the number of samples in the block. The return value is 0 if the read
    // started at the end of stream, or a number in the range [1, 65536] for a valid block.
    // All metadata blocks must be read before starting to read audio blocks.
    fun readAudioBlock(samples: Array<IntArray>, off: Int): Int {
        checkNotNull(frameDec) { "Metadata blocks not fully consumed yet" }
        val frame = frameDec!!.readFrame(samples, off)
        return frame?.blockSize ?: 0 // In the range [1, 65536]
    }

    // Seeks to the given sample position and reads audio samples into the given buffer,
    // returning the number of samples filled. If audio data is available then the return value
    // is at least 1; otherwise 0 is returned to indicate the end of stream. Note that the
    // sample position can land in the middle of a FLAC block and will still behave correctly.
    // In theory this method subsumes the functionality of readAudioBlock(), but seeking can be
    // an expensive operation so readAudioBlock() should be used for ordinary contiguous streaming.
    fun seekAndReadAudioBlock(pos: Long, samples: Array<IntArray>, off: Int): Int {
        checkNotNull(frameDec) { "Metadata blocks not fully consumed yet" }
        var sampleAndFilePos: LongArray? = getBestSeekPoint(pos)
        if (pos - sampleAndFilePos!![0] > 300000) {
            sampleAndFilePos = seekBySyncAndDecode(pos)
            sampleAndFilePos!![1] -= metadataEndPos
        }
        input!!.seekTo(sampleAndFilePos!![1] + metadataEndPos)
        var curPos = sampleAndFilePos[0]
        val smpl = Array(streamInfo!!.numChannels) { IntArray(65536) }
        while (true) {
            val frame = frameDec!!.readFrame(smpl, 0) ?: return 0
            val nextPos = curPos + frame.blockSize
            if (nextPos > pos) {
                for (ch in smpl.indices) {
                    arraycopy(
                        smpl[ch],
                        (pos - curPos).toInt(),
                        samples[ch],
                        off,
                        (nextPos - pos).toInt()
                    )
                }
                return (nextPos - pos).toInt()
            }
            curPos = nextPos
        }
    }

    private fun getBestSeekPoint(pos: Long): LongArray {
        var samplePos: Long = 0
        var filePos: Long = 0
        if (seekTable != null) {
            for (p in seekTable!!.points) {
                if (p!!.sampleOffset <= pos) {
                    samplePos = p.sampleOffset
                    filePos = p.fileOffset
                } else break
            }
        }
        return longArrayOf(samplePos, filePos)
    }

    // Returns a pair (sample offset, file position) such sampleOffset <= pos and abs(sampleOffset - pos)
    // is a relatively small number compared to the total number of samples in the audio file.
    // This method works by skipping to arbitrary places in the file, finding a sync sequence,
    // decoding the frame header, examining the audio position stored in the frame, and possibly deciding
    // to skip to other places and retrying. This changes the state of the input streams as a side effect.
    // There is a small chance of finding a valid-looking frame header but causing erroneous decoding later.
    private fun seekBySyncAndDecode(pos: Long): LongArray? {
        var start = metadataEndPos
        var end = input.length
        while (end - start > 100000) {  // Binary search
            val mid = start + end ushr 1
            val offsets = getNextFrameOffsets(mid)
            if (offsets == null || offsets[0] > pos) end = mid else start = offsets[1]
        }
        return getNextFrameOffsets(start)
    }

    // Returns a pair (sample offset, file position) describing the next frame found starting
    // at the given file offset, or null if no frame is found before the end of stream.
    // This changes the state of the input streams as a side effect.
    private fun getNextFrameOffsets(filePos: Long): LongArray? {
        var filePos = filePos
        require(!(filePos < metadataEndPos || filePos > input.length)) { "File position out of bounds" }

        // Repeatedly search for a sync
        while (true) {
            input!!.seekTo(filePos)

            // Finite state machine to match the 2-byte sync sequence
            var state = 0
            while (true) {
                val b = input!!.readByte()
                state =
                    if (b == -1) return null else if (b == 0xFF) 1 else if (state == 1 && b and 0xFE == 0xF8) break else 0
            }

            // Sync found, rewind 2 bytes, try to decode frame header
            filePos = input!!.position - 2
            input!!.seekTo(filePos)
            filePos += try {
                val frame: FrameInfo = FrameInfo.Companion.readFrame(input) ?: error("Can't read frame")
                return longArrayOf(getSampleOffset(frame), filePos)
            } catch (e: DataFormatException) {
                // Advance past the sync and search again
                2
            }
        }
    }

    // Calculates the sample offset of the given frame, automatically handling the constant-block-size case.
    private fun getSampleOffset(frame: FrameInfo): Long {
        return if (frame!!.sampleOffset != -1L) frame.sampleOffset else if (frame.frameIndex != -1) (frame.frameIndex * streamInfo!!.maxBlockSize).toLong() else throw AssertionError()
    }

    // Closes the underlying input streams and discards object data.
    // This decoder object becomes invalid for any method calls or field usages.
    override fun close() {
        streamInfo = null
        seekTable = null
        frameDec = null
        input.close()
    }
}
