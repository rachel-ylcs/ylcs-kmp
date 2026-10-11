package love.yinlin.foundation.compression

/** 输入提前结束，无法读取完整的压缩数据。 */
class CompressionTruncatedInputException(message: String, offset: Int = -1) : CompressionException(message, offset)