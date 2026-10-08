package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.Page
import love.yinlin.GalleryActions
import love.yinlin.GalleryBadge
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.navigation.Breadcrumb
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.input.TextButton

@Stable
object NavigationPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("TabBar") {
                var currentIndex by rememberValueState(0)

                GalleryBadge("当前选中 · Item $currentIndex")
                TabBar(
                    size = 5,
                    index = currentIndex,
                    onNavigate = { currentIndex = it },
                    titleProvider = { "Item $it" },
                    iconProvider = { if (it == 2) Icons.Home else null },
                    enabledProvider = { it != 3 }
                )
            }

            Component("Breadcrumb") {
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
