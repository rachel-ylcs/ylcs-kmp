package love.yinlin.foundation.compression

sealed class CompressionException(message: String, val offset: Int = -1) : IllegalArgumentException(if (offset < 0) message else "$message at byte $offset.")
