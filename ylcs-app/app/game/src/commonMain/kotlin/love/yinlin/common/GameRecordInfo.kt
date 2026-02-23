package love.yinlin.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement

@Stable
fun interface GameRecordInfo {
    @Stable
    data class Data(val answer: JsonElement, val info: JsonElement)

    @Composable
    fun ColumnScope.GameRecordInfoContent(data: Data)
}