package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
expect class ByteArrayCompatible(raw: ByteArray) {
    val raw: ByteArray
    val size: Int
    operator fun get(index: Int): Byte
    operator fun set(index: Int, value: Byte)
    operator fun iterator(): ByteIterator
    val asWebByteArray: WebByteArray
}