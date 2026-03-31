package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable

/**
 * 标识
 */
@Stable
interface Identifiable<T : Comparable<T>> : Comparable<Identifiable<T>> {
    /**
     * 唯一标识符
     */
    val id: T

    /**
     * 可读ID
     */
    val idString: String
}