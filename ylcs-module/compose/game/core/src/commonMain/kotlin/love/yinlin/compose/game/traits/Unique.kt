package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Stable
abstract class Unique : Identifiable<Uuid> {
    final override val id: Uuid = Uuid.generateV7()
    override val idString: String get() = id.toString()
    final override fun compareTo(other: Identifiable<Uuid>): Int = this.id.compareTo(other.id)
    final override fun hashCode(): Int = id.hashCode()
    final override fun equals(other: Any?): Boolean = (other as? Unique)?.id == this.id
    override fun toString(): String = idString
}