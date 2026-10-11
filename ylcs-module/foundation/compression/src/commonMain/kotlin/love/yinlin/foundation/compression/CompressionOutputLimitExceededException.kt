package love.yinlin.foundation.compression

/** 输出超过配置的解压长度限制。 */
class CompressionOutputLimitExceededException(message: String, offset: Int = -1) : CompressionException(message, offset)