package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.Page
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
        }
    }
}