package love.yinlin.common

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.platform.OS
import love.yinlin.platform.Platform

@Serializable(Scheme.Serializer::class)
data class Scheme(val name: String) {
    companion object {
        val Http = Scheme("http")
        val Https = Scheme("https")
        val File = Scheme("file")
        val Content = Scheme("content")
        val Package = Scheme("scheme")
        val Rachel = Scheme("rachel")
        val NetEaseCloud = Scheme("nec")
        val QQMusic = Scheme("qm")
        val Taobao = Scheme("taobao")
        val QQ = Scheme("mqqapi")
    }

    override fun toString(): String = name

    object Serializer : KSerializer<Scheme> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.Scheme", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Scheme) = encoder.encodeString(value.name)
        override fun deserialize(decoder: Decoder): Scheme = Scheme(decoder.decodeString())
    }
}

@Stable
@Serializable
data class Uri(
    val scheme: Scheme,
    val host: String? = null,
    val port: Int? = null,
    val path: String? = null,
    val query: String? = null
) {
    fun encode(): String = encodeUri(toString())

    val params: Map<String, String> get() {
        val map = mutableMapOf<String, String>()
        val items = query?.split('&') ?: emptyList()
        for (item in items) {
            val key = item.substringBefore('=')
            val value = item.substringAfter('=')
            if (item.contains('=') && key.isNotEmpty()) map[key] = value
        }
        return map
    }

    override fun toString(): String = buildString {
        append("$scheme://")
        if (host != null) append(host)
        if (port != null) append(":$port")
        if (path != null) append(path)
        if (query != null) append("?$query")
    }

    companion object {
        val Empty: Uri = Uri(Scheme(""))

        fun parse(uri: String): Uri? = parseUri(uri)

        private val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        private val defaultSet = charArrayOf('_', '-', '!', '.', '~', '\'', '(', ')', '*')
        private fun isAllowed(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in defaultSet

        private class ByteBuffer(private val size: Int) {
            private val buffer by lazy { ByteArray(size) }

            var writePosition = 0
                private set

            fun writeByte(byte: Byte) {
                buffer[writePosition++] = byte
            }

            fun decodeToStringAndReset() = try {
                buffer.decodeToString(startIndex = 0, endIndex = writePosition, throwOnInvalidSequence = false)
            } finally {
                writePosition = 0
            }

            fun flushDecodingByteAccumulator(builder: StringBuilder) {
                if (writePosition == 0) return
                try {
                    builder.append(decodeToStringAndReset())
                }
                catch (_: Exception) {
                    builder.append('\ufffd')
                }
            }
        }

        private fun parseUri(uri: String): Uri? {
            val schemeEnd = uri.indexOf(':')
            if (schemeEnd == -1) return null
            val scheme = uri.take(schemeEnd)
            if (scheme.isEmpty()) return null
            if (schemeEnd + 2 >= uri.length || uri[schemeEnd + 1] != '/' || uri[schemeEnd + 2] != '/') return null
            val remaining = uri.substring(schemeEnd + 3)
            var authorityEnd = -1
            for (i in remaining.indices) {
                when (remaining[i]) {
                    '/', '?' -> {
                        authorityEnd = i
                        break
                    }
                }
            }
            val authorityPart = if (authorityEnd == -1) remaining else remaining.take(authorityEnd)
            var afterAuthority = if (authorityEnd == -1) "" else remaining.substring(authorityEnd)
            var host: String? = null
            var port: Int? = null
            if (authorityPart.isNotEmpty()) {
                val lastColonIndex = authorityPart.lastIndexOf(':')
                if (lastColonIndex != -1) {
                    val hostCandidate = authorityPart.take(lastColonIndex)
                    if (hostCandidate.isEmpty()) return null
                    val portStr = authorityPart.substring(lastColonIndex + 1)
                    if (portStr.isEmpty()) return null
                    val portNum = portStr.toIntOrNull() ?: return null
                    host = hostCandidate
                    port = portNum
                } else {
                    host = authorityPart
                }
            }
            var query: String? = null
            val queryIndex = afterAuthority.indexOf('?')
            if (queryIndex != -1) {
                query = afterAuthority.substring(queryIndex + 1)
                afterAuthority = afterAuthority.take(queryIndex)
            }
            val path: String? = afterAuthority.ifEmpty { null }
            return Uri(Scheme(scheme.lowercase()), host, port, path, query)
        }

        fun encodeUri(str: String): String {
            val encoded = StringBuilder()
            val oldLength: Int = str.length
            var current = 0
            while (current < oldLength) {
                var nextToEncode = current
                while (nextToEncode < oldLength && isAllowed(str[nextToEncode])) nextToEncode++
                if (nextToEncode == oldLength) {
                    return if (current == 0) str else {
                        encoded.append(str, current, oldLength)
                        encoded.toString()
                    }
                }
                if (nextToEncode > current) encoded.append(str, current, nextToEncode)
                current = nextToEncode
                var nextAllowed = current + 1
                while (nextAllowed < oldLength && !isAllowed(str[nextAllowed])) nextAllowed++
                val toEncode = str.substring(current, nextAllowed)
                try {
                    val bytes: ByteArray = toEncode.encodeToByteArray()
                    val bytesLength = bytes.size
                    for(i in 0 until bytesLength) {
                        encoded.append('%')
                        encoded.append(hexDigits[bytes[i].toInt() and 0xf0 shr 4])
                        encoded.append(hexDigits[bytes[i].toInt() and 0xf])
                    }
                } catch (_: Exception) {
                    return str
                }
                current = nextAllowed
            }
            return encoded.toString()
        }

        fun decodeUri(str: String): String {
            val length = str.length
            val builder = StringBuilder(length)
            ByteBuffer(length).apply {
                var i = 0
                while (i < length) {
                    when (val c = str[i++]) {
                        '+' -> {
                            flushDecodingByteAccumulator(builder)
                            builder.append('+')
                        }
                        '%' -> {
                            var hexValue: Byte = 0
                            for (@Suppress("Unused") j in 0..1) {
                                if (i >= length) {
                                    flushDecodingByteAccumulator(builder)
                                    builder.append('\ufffd')
                                    return builder.toString()
                                }
                                val newDigit: Int = when (val nextC = str[i++]) {
                                    in '0'..'9' -> nextC.code - '0'.code
                                    in 'a'..'f' -> 10 + nextC.code - 'a'.code
                                    in 'A'..'F' -> 10 + nextC.code - 'A'.code
                                    else -> -1
                                }
                                if (newDigit < 0) {
                                    flushDecodingByteAccumulator(builder)
                                    builder.append('\ufffd')
                                    break
                                }
                                hexValue = (hexValue * 0x10 + newDigit).toByte()
                            }
                            writeByte(hexValue)
                        }
                        else -> {
                            flushDecodingByteAccumulator(builder)
                            builder.append(c)
                        }
                    }
                }
                flushDecodingByteAccumulator(builder)
            }
            return builder.toString()
        }
    }
}

object UriGenerator {
    fun qq(id: String): Uri = Uri(
        scheme = Scheme.QQ,
        host = "card",
        path = "/show_pslcard",
        query = "src_type=internal&version=1&uin=$id&card_type=person&source=qrcode"
    )

    fun qqGroup(id: String) = Uri(
        scheme = Scheme.QQ,
        host = "card",
        path = "/show_pslcard",
        query = "src_type=internal&version=1&uin=$id&card_type=group&source=qrcode"
    )

    fun qqGroup(k: String, authKey: String) = Uri(
        scheme = Scheme.Https,
        host = "qm.qq.com",
        path = "/cgi-bin/qm/qr",
        query = "k=$k&authKey=$authKey"
    )

    fun taobao(shopId: String): Uri = OS.ifPlatform(
        *Platform.Phone,
        ifTrue = { Uri(
            scheme = Scheme.Taobao,
            host = "shop.m.taobao.com",
            path = "/shop/shop_index.htm",
            query = "shop_id=$shopId"
        ) },
        ifFalse = {
            Uri(
                scheme = Scheme.Https,
                host = "shop$shopId.taobao.com"
            )
        }
    )
}