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
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Text

@Stable
object PlatformViewPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("原生组件嵌入") {
                val name = platformViewName
                if (name == null) PlatformViewExample("", Modifier.fillMaxWidth())
                else {
                    var revision by rememberValueState(0)
                    var mounted by rememberTrue()
                    var showOverlay by rememberTrue()
                    var overlayClicks by rememberValueState(0)

                    GalleryBadge(name)
                    GalleryActions {
                        TextButton("更新原生内容", onClick = { revision++ })
                        TextButton(if (mounted) "移除原生组件" else "重新挂载组件", onClick = { mounted = !mounted })
                        TextButton(if (showOverlay) "隐藏 Compose 浮层" else "显示 Compose 浮层", onClick = { showOverlay = !showOverlay })
                    }
                    Box(
                        Modifier.fillMaxWidth().height(Theme.size.cell2)
                            .border(Theme.border.v7, Theme.color.outline)
                            .background(Theme.color.backgroundVariant),
                    ) {
                        if (mounted) {
                            PlatformViewExample(
                                text = "$name\n这是平台原生文本组件 · 参数更新 $revision\n可直接输入中文和英文。",
                                modifier = Modifier.matchParentSize().padding(Theme.padding.value),
                            )
                            if (showOverlay) {
                                TextButton(
                                    text = "Compose 浮层 · 点击 $overlayClicks 次",
                                    color = Theme.color.onContainer,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(Theme.padding.value)
                                        .background(Theme.color.primaryContainer, Theme.shape.v3),
                                    onClick = { overlayClicks++ },
                                )
                            }
                        }
                        else Text("原生组件已移除", modifier = Modifier.align(Alignment.Center))
                    }
                    GalleryParagraph("原生区域可以直接编辑；普通重组保留输入内容，点击“更新原生内容”才会写入新的参数。移除或离开本页会释放原生组件，重新挂载会创建新实例。")
                    GalleryParagraph("Compose 按钮覆盖在原生组件上，点击时只应增加上方计数。也可以滚动页面或调整窗口大小，检查原生区域的裁剪和位置。")
                }
            }
        }
    }
}
