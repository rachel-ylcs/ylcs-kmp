package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.extension.catchingNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Stable
@Serializable
data class MusicResource(
    val id: Int,
    val name: String
) {
    init {
        require(check(id, name)) { "资源名称非法: $id-$name" }
    }

    val type: MusicResourceType? by lazy { MusicResourceType.fromInt(id) }

    override fun toString(): String = "$id-$name"

    companion object {
        @OptIn(ExperimentalContracts::class)
        private fun check(id: Int?, name: String): Boolean {
            contract {
                returns(true) implies (id != null)
            }

            return id != null && id >= 0 && name.length in 1 .. 32
        }

        fun fromString(text: String): MusicResource? = catchingNull {
            val list = text.split("-")
            MusicResource(list[0].toInt(), list[1])
        }
    }
}