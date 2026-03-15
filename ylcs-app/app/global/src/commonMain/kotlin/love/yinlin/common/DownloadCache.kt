package love.yinlin.common

import love.yinlin.app
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.cs.NetClient
import love.yinlin.fs.*

fun urlDigest32(s: String): String {
    fun mix64(x: Long): Long {
        var z = x
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
    fun toBase26(x: Long): String = buildString(8) {
        var u = x.toULong()
        repeat(8) {
            val idx = (u % 26U).toInt()
            append(('a'.code + idx).toChar())
            u /= 26U
        }
    }
    var h = -0x61c8864680b583ebL
    for (c in s) h = mix64(h xor c.code.toLong())
    return buildString(32) {
        var v = h
        repeat(4) {
            v = mix64(v)
            append(toBase26(v))
        }
    }
}

@IOCoroutine
suspend fun NetClient.downloadCacheWithPath(url: String): File? {
    val path = File(app.cachePath, urlDigest32(url))
    return if (path.fileSize() > 0L) path
    else if (simpleDownload(url, path.bufferedSink()) && path.fileSize() > 0L) path
    else null
}

@IOCoroutine
suspend fun NetClient.downloadCache(url: String): ByteArray? {
    val path = File(app.cachePath, urlDigest32(url))
    var result: ByteArray? = null
    if (path.fileSize() > 0L) result = path.readByteArray()
    else if (simpleDownload(url, path.bufferedSink())) result = path.readByteArray()
    return if (result?.isEmpty() ?: true) null else result
}