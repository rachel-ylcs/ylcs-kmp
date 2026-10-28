package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.GalleryActions
import love.yinlin.GalleryBadge
import love.yinlin.GalleryCaption
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dragDrop
import love.yinlin.compose.ui.text.Text

@Stable
object DragDropPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            GalleryParagraph("在 JVM 或 Native 桌面端，从系统文件管理器拖入文件，或从其他应用拖入选中的文本。Web 端的文件结果仅展示文件名；Android 与 iOS 不接收拖放。")

            Component("文件拖放") {
                DropExample(DragFlag.File, "将一个或多个文件拖到这里")
            }
            Component("文本拖放") {
                DropExample(DragFlag.Text, "将其他应用中选中的文本拖到这里")
            }
            Component("混合接收与开关") {
                var enabled by rememberTrue()
                var acceptFile by rememberTrue()
                var acceptText by rememberTrue()

                GalleryActions {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text("启用拖放")
                    Switch(checked = acceptFile, onCheckedChange = { acceptFile = it })
                    Text("文件")
                    Switch(checked = acceptText, onCheckedChange = { acceptText = it })
                    Text("文本")
                }

                val flag = when {
                    acceptFile && acceptText -> DragFlag.File or DragFlag.Text
                    acceptFile -> DragFlag.File
                    acceptText -> DragFlag.Text
                    else -> DragFlag.File and DragFlag.Text
                }
                DropExample(flag, "拖入文件或文本，检查当前过滤条件", enabled)
            }
        }
    }

    @Composable
    private fun DropExample(flag: DragFlag, prompt: String, enabled: Boolean = true) {
        var result by rememberState<DropResult?> { null }
        var count by rememberValueState(0)
        val available = enabled && flag.value != 0

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = Theme.size.cell4)
                .background(if (available) Theme.color.backgroundVariant else Theme.color.disabledContainer, Theme.shape.v3)
                .border(Theme.border.v7, Theme.color.outline, Theme.shape.v3)
                .dragDrop(enabled, flag) {
                    result = it
                    count++
                }
                .padding(Theme.padding.value),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (available) prompt else "当前不接收拖放", color = if (available) Theme.color.onBackgroundVariant else Theme.color.disabledContent)
        }

        GalleryActions {
            GalleryBadge("已接收 $count 次")
            TextButton("清空记录", icon = Icons.Refresh, enabled = result != null) {
                result = null
                count = 0
            }
        }

        when (val received = result) {
            is DropResult.File -> {
                GalleryCaption("文件 · ${received.path.size} 个")
                for (file in received.path.take(20)) Text(file.path)
                if (received.path.size > 20) GalleryCaption("仅展示前 20 个文件")
            }
            is DropResult.Text -> {
                GalleryCaption("文本 · ${received.text.length} 个字符")
                Text(received.text.take(4096))
                if (received.text.length > 4096) GalleryCaption("仅展示前 4096 个字符")
            }
            null -> GalleryCaption("尚未收到内容；拖入的文件不会被读取或修改。")
        }
    }
}
