package love.yinlin.foundation.compression

/** 压缩数据不符合格式或包含非法长度、匹配距离等。 */
class CompressionInvalidDataException(message: String, offset: Int = -1) : CompressionException(message, offset)