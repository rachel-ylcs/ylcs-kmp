package love.yinlin.page

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import love.yinlin.Page
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.collection.TreeView
import love.yinlin.compose.ui.icon.Icons

@Stable
object CollectionPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("TreeView") {
                TreeView {
                    TreeNode(text = "输入", icon = Icons.CheckBox) {
                        TreeNode(text = "CheckBox") {
                            TreeNode("TriStateCheckBox")
                        }
                        TreeNode(text = "Button", icon = Icons.Check)
                        TreeNode(text = "Switch")
                    }
                    TreeNode(text = "集合", icon = Icons.GridOn) {
                        TreeNode(text = "TreeView")
                    }
                    TreeNode(text = "主题", icon = Icons.Theme)
                    TreeNode(text = "文本", icon = Icons.TextFields)
                }
            }

            Component("TagView") {
                val items = remember { (1 .. 50).map { "Item $it" }.toMutableStateList() }

                TagView(
                    modifier = Modifier.fillMaxWidth(),
                    size = items.size,
                    titleProvider = { items[it] },
                    iconProvider = { if (it % 7 == 0) Icons.Tag else null },
                    onDelete = { items.removeAt(it) }
                )
            }
        }
    }
}