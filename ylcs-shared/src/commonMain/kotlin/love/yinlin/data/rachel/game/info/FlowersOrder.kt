package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.game.ExplorationConfig

@Stable
@Suppress("MayBeConstant")
data object FOConfig : ExplorationConfig() {
    val minLength: Int = 10 // 最小长度
    val maxLength: Int = 14 // 最大长度
}

@Stable
@Serializable
data class FOInfo(
    val tryCount: Int, // [尝试次数]
)

@Stable
enum class FOType {
    CORRECT, // 正确
    INVALID_POS, // 位置错误
    INCORRECT; // 错误

    companion object {
        fun encode(items: List<FOType>): Int {
            var encoded = 0
            val countOffset = items.size - 10
            encoded = encoded or (countOffset shl 28)
            items.forEachIndexed { index, state ->
                encoded = encoded or (state.ordinal shl (index * 2))
            }
            return encoded
        }

        fun decode(value: Int): List<FOType> {
            val countOffset = (value ushr 28) and 0x07
            val count = countOffset + 10
            return List(count) { index ->
                when ((value ushr (index * 2)) and 0x03) {
                    CORRECT.ordinal -> CORRECT
                    INVALID_POS.ordinal -> INVALID_POS
                    else -> INCORRECT
                }
            }
        }

        fun verify(value: Int): Boolean = value and 0x0FFFFFFF == 0
    }
}