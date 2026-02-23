package love.yinlin.data.rachel.literal

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate

@Stable
internal data class AppUpdateRecordGroup(
    val type: String,
    val icon: ImageVector,
    val color: Color,
    val background: Color,
    val records: List<String>
)

@Stable
internal data class AppUpdateInfo(
    val platform: String,
    val title: String?,
    val force: Boolean,
    val maintenance: Boolean,
    val date: LocalDate,
    val groups: List<AppUpdateRecordGroup>
)