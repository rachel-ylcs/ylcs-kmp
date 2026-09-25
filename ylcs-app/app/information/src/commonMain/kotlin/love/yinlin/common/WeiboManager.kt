package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.ui.icon.Icons2

@Stable
class WeiboManager : MessageManager() {
    override val name: String = "微博"
    override val icon: ImageVector = Icons2.Weibo
    override val pageable: Boolean = false
}