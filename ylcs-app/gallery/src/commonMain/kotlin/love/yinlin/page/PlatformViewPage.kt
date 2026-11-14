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
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Text

@Stable
object PlatformViewPage : Page(
    id = "platform-view",
    icon = Icons.Package,
    title = "原生视图",
    english = "Interop",
    description = "在 Compose 界面中嵌入平台原生组件。",
    keywords = "Platform View 原生控件",
    group = PageGroup.Layout,
) {
    private val demoPlatformView = component("Interop", "可直接编辑原生文本，也可在上方叠放 Compose 控件。", "PlatformViewState PlatformViewHost", previewAlias = "原生组件嵌入")

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(demoPlatformView) {
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
                }
            }
        }
    }
}
