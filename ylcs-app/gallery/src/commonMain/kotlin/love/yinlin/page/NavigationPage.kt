package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.GalleryActions
import love.yinlin.GalleryBadge
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.navigation.Breadcrumb
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.navigation.Segment

@Stable
object NavigationPage : Page(
    id = "navigation",
    icon = Icons.Anchor,
    title = "导航",
    english = "Navigation",
    description = "切换同级内容，或沿路径返回上一级。",
    keywords = "标签 面包屑",
    group = PageGroup.Interaction,
) {
    private val demoTabBar = component("TabBar", "用标签切换同级页面。", "标签栏")
    private val demoSegment = component("Segment", "在几个并排的选项之间切换。", "分段")
    private val demoBreadcrumb = component("Breadcrumb", "显示当前位置，点击路径可返回上层。", "面包屑")

    @Composable
    override fun Content() {
        ComponentColumn {
            var currentIndex by rememberValueState(0)

            Component(demoTabBar) {
                TabBar(
                    size = 5,
                    index = currentIndex,
                    onNavigate = { currentIndex = it },
                    titleProvider = { "Item $it" },
                    iconProvider = { if (it == 2) Icons.Home else null },
                    enabledProvider = { it != 3 }
                )
            }

            Component(demoSegment) {
                Segment(
                    size = 5,
                    index = currentIndex,
                    onNavigate = { currentIndex = it },
                    titleProvider = { "Item $it" },
                    iconProvider = { if (it == 2) Icons.Home else null }
                )
            }

            Component(demoBreadcrumb) {
                var items by rememberState { ["Home", "Documents", "Code", "Kotlin", "Compose"] }

                GalleryActions {
                    GalleryBadge("当前路径 · ${items.last()}")
                    TextButton("重置路径", icon = Icons.Refresh) { items = ["Home", "Documents", "Code", "Kotlin", "Compose"] }
                }
                Breadcrumb(
                    size = items.size,
                    onNavigate = { items = items.take(it + 1) },
                    titleProvider = { items[it] },
                    iconProvider = {
                        when (items[it]) {
                            "Home" -> Icons.Home
                            "Code" -> Icons.Code
                            else -> null
                        }
                    },
                )
            }
        }
    }
}
