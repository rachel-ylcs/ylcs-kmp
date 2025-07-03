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

import korlibs.memory.*

/**
 * A FLAC input stream based on a fixed byte array.
 */
class ByteArrayFlacInput(b: ByteArray) : AbstractFlacLowLevelInput() {
    /*---- Constructors ----*/
    /*---- Fields ----*/ // The underlying byte array to read from.
    private var data: ByteArray? = b
    private var offset = 0

    /*---- Methods ----*/
    override val length: Long
        get() = data!!.size.toLong()

    override fun seekTo(pos: Long) {
        offset = pos.toInt()
        positionChanged(pos)
    }

    override fun readUnderlying(buf: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || off > buf!!.size || len < 0 || len > buf.size - off) throw IndexOutOfBoundsException()
        val n = kotlin.math.min(data!!.size - offset, len)
        if (n == 0) return -1
        arraycopy(data!!, offset, buf, off, n)
        offset += n
        return n
    }

    // Discards data buffers and invalidates this stream. Because this class and its superclass
    // only use memory and have no native resources, it's okay to simply let a ByteArrayFlacInput
    // be garbage-collected without calling close().
    override fun close() {
        if (data == null) return
        data = null
        super.close()
    }
}
