package love.yinlin.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
abstract class MessageManager {
    abstract val name: String // 名称
    abstract val icon: ImageVector // 图标
    abstract val pageable: Boolean // 可分页

    open suspend fun onSettingsOpen() { }

    @Composable
    open fun ColumnScope.SettingsLayout() { }
}