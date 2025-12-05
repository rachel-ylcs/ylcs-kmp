package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.extension.bufferedSink
import love.yinlin.extension.isFile
import love.yinlin.extension.readByteArray
import love.yinlin.extension.size
import love.yinlin.platform.NetClient

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

suspend fun NetClient.downloadCacheWithPath(url: String): Path? {
    val path = Path(app.os.storage.cachePath, urlDigest32(url))
    return if (path.isFile && path.size > 0L) path
    else if (simpleDownload(url, path.bufferedSink) && path.size > 0L) path
    else null
}

suspend fun NetClient.downloadCache(url: String): ByteArray? {
    val path = Path(app.os.storage.cachePath, urlDigest32(url))
    var result: ByteArray? = null
    if (path.isFile && path.size > 0L) result = path.readByteArray()
    else if (simpleDownload(url, path.bufferedSink)) result = path.readByteArray()
    return if (result?.isEmpty() ?: true) null else result
}