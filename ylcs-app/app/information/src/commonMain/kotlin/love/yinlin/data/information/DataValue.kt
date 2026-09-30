package love.yinlin.data.information

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class DataValue(
    val title: String,
    val value: String,
    val icon: ImageVector
)