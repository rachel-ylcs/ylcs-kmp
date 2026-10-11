package love.yinlin.foundation.compression

/** 压缩方法、预置字典等格式特性不受支持。 */
class CompressionUnsupportedFormatException(message: String, offset: Int = -1) : CompressionException(message, offset)