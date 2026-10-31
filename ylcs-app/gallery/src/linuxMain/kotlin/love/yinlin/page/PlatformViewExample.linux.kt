package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.GalleryParagraph

internal actual val platformViewName: String? = null

@Composable
internal actual fun PlatformViewExample(text: String, modifier: Modifier) {
    GalleryParagraph("Linux Native 暂未接入原生组件嵌入；此演示可在 Windows Native、macOS Native、JVM 和 Web 查看。", modifier)
}
