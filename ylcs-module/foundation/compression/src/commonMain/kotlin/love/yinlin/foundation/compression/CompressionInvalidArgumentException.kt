package love.yinlin.foundation.compression

/** 调用参数无效。 */
class CompressionInvalidArgumentException(message: String, offset: Int = -1) : CompressionException(message, offset)