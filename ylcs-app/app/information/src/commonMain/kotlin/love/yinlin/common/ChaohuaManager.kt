package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.ui.icon.Icons2

@Stable
class ChaohuaManager : MessageManager() {
    override val name: String = "超话"
    override val icon: ImageVector = Icons2.Chaohua
    override val pageable: Boolean = true
}