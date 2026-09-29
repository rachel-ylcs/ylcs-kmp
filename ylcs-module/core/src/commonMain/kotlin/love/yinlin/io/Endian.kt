package love.yinlin.io

import kotlinx.serialization.Serializable

@Serializable
enum class Endian {
    BIG, // 大端序
    LITTLE; // 小端序
}