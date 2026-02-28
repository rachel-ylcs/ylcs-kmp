package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable

@Stable
interface RichDrawer {
    val type: String

    fun RichRenderScope.render(item: RichObject)
}