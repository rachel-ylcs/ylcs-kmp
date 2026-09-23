package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class RhymeRepository(
    val characters: List<Int> = [], // 角色数据
)