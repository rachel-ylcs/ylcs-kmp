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

/**
 * A FLAC input stream based on a [SyncRandomAccessFile].
 */
class SeekableFileFlacInput(val stream: SyncStream) : AbstractFlacLowLevelInput() {
    /*---- Constructors ----*/
    /*---- Fields ----*/ // The underlying byte-based input stream to read from.

    /*---- Methods ----*/
    override val length: Long
        get() = try {
            stream.length
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    override fun seekTo(pos: Long) {
        stream.position = pos
        positionChanged(pos)
    }

    override fun readUnderlying(buf: ByteArray, off: Int, len: Int): Int {
        return stream.read(buf, off, len)
    }

    // Closes the underlying RandomAccessFile stream (very important).
    override fun close() {
        stream.close()
        super.close()
    }
}
