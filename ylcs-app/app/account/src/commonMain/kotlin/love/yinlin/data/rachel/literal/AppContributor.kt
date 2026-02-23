package love.yinlin.data.rachel.literal

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
internal data class AppContributor(
    val name: String,
    val uid: Int
)

@Stable
internal data class AppContributorGroup(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val names: List<AppContributor>
)