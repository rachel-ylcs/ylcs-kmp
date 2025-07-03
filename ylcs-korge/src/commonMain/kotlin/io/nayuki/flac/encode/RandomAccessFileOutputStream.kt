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
import io.nayuki.flac.common.*
import korlibs.io.stream.*
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.*

/* 
 * An adapter from RandomAccessFile to OutputStream. These objects have no buffer, so seek()
 * and write() can be safely interleaved. Also, objects of this class have no direct
 * native resources - so it is safe to discard a RandomAccessFileOutputStream object without
 * closing it, as long as other code will close() the underlying RandomAccessFile object.
 */
class RandomAccessFileOutputStream(raf: SyncRandomAccessFile) : SyncOutputStream {
    /*---- Fields ----*/
    private var out: RandomAccessFile = raf

    /*---- Methods ----*/
    fun getPosition(): Long {
        return out!!.getFilePointer()
    }

    fun seek(pos: Long) {
        out!!.seek(pos)
    }



    public override fun write(b: Int) {
        out!!.write(b)
    }

    public override fun write(b: ByteArray, off: Int, len: Int) {
        out!!.write(b, off, len)
    }

    public override fun close() {
        if (out != null) {
            out!!.close()
            out = null
        }
    }
}
*/
