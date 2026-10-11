package love.yinlin.foundation.compression

/** 调用方提供的目标数组容量不足。 */
class CompressionOutputTooSmallException(message: String, offset: Int = -1) : CompressionException(message, offset)