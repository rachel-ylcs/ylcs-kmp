package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
enum class RhymeDifficulty(val title: String) {
    Easy("简单"),
    Medium("普通"),
    Hard("困难"),
    Extreme("极限");

    companion object {
        fun fromInt(value: Int): RhymeDifficulty = when (value) {
            Medium.ordinal -> Medium
            Hard.ordinal -> Hard
            Extreme.ordinal -> Extreme
            else -> Easy
        }
    }
}