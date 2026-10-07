package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.StringSerializer
import love.yinlin.reflect.metaClassName
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, CompatibleRachelApi::class)
@Stable
@Serializable(ScreenID.Serializer::class)
internal data class ScreenID(internal val id: Uuid = Uuid.generateV7()) {
    object Serializer : StringSerializer<ScreenID>(metaClassName<ScreenID>()) {
        override fun encode(value: ScreenID): String = value.id.toString()
        override fun decode(value: String): ScreenID = ScreenID(Uuid.parse(value))
    }

    override fun equals(other: Any?): Boolean = id == (other as? ScreenID)?.id
    override fun toString(): String = id.toString()
    override fun hashCode(): Int = id.hashCode()
}
