package love.yinlin.foundation.compression

interface CompressionCodec {
    // 压缩后的长度上界，用于预分配缓冲区
    fun maxCompressedSize(sourceSize: Int): Int

    // 分配输出数组并返回实际压缩数据
    fun compress(data: ByteArray): ByteArray

    // 分配输出数组并返回解压数据，受算法实例的 maxDecompressedSize 限制
    fun decompress(data: ByteArray): ByteArray

    /**
     * 从 dstOffset 开始写入压缩数据，返回实际写入长度，未使用的区域保持不变。
     * src 与 dst 不能是同一个数组；异常发生时 dst 可能已被部分写入。
     * @throws CompressionInvalidArgumentException 数组相同或目标偏移无效。
     * @throws CompressionOutputTooSmallException 目标数组的剩余容量不足。
     */
    fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int = 0): Int

    /**
     * 从 dstOffset 开始写入解压数据，返回实际写入长度，未使用的区域保持不变。
     * src 与 dst 不能是同一个数组；异常发生时 dst 可能已被部分写入。
     * 解压长度不一定能从压缩数据提前计算，调用方可提供容量上界并处理容量不足异常。
     * @throws CompressionInvalidArgumentException 数组相同或目标偏移无效。
     * @throws CompressionOutputTooSmallException 目标数组的剩余容量不足。
     * @throws CompressionOutputLimitExceededException 超过算法实例配置的解压长度限制。
     * @throws CompressionException 压缩数据损坏、截断、校验失败或格式不受支持。
     */
    fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int = 0): Int

    companion object {
        internal const val DEFAULT_DECOMPRESSION_LIMIT = 64 * 1024 * 1024

        internal fun checkDecompressionLimit(limit: Int) {
            if (limit < 0) throw CompressionInvalidArgumentException("Negative decompression limit")
        }

        internal fun compressedBound(sourceSize: Int, overhead: Long): Int {
            if (sourceSize < 0 || sourceSize.toLong() + overhead > Int.MAX_VALUE) {
                throw CompressionInvalidArgumentException("Source size exceeds the supported range")
            }
            return (sourceSize + overhead).toInt()
        }
    }
}