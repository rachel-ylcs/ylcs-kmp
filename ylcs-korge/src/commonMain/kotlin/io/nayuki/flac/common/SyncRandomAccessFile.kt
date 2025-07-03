package io.nayuki.flac.common

class SyncRandomAccessFile(val file: SyncFile, val mode: String) : AutoCloseable {
    fun length(): Long = TODO()
    fun seek(pos: Long) {
        TODO()
    }
    fun read(buf: ByteArray, off: Int, len: Int): Int {
        TODO()
    }
    override fun close() {
        TODO()
    }
}
