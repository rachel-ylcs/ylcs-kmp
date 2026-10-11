package love.yinlin.foundation.compression

/** 校验和不匹配。 */
class CompressionChecksumMismatchException(message: String, offset: Int = -1) : CompressionException(message, offset)