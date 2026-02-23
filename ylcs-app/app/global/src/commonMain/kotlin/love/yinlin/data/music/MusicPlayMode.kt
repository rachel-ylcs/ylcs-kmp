package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class MusicPlayMode {
    ORDER, LOOP, RANDOM;

    val next: MusicPlayMode get() = when (this) {
        ORDER -> LOOP
        LOOP -> RANDOM
        RANDOM -> ORDER
    }

    companion object {
        val DEFAULT = ORDER
    }
}