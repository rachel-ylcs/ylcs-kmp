package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.TextUnit

@Stable
class RichRenderer(
    drawers: List<RichDrawer>,
    private val onAction: (RichObject) -> Unit,
) {
    private val renderDrawers = mutableStateMapOf<String, RichDrawer>().apply {
        put(RichType.Text.value, RichNodeText.Drawer)
        put(RichType.Br.value, RichNodeBr.Drawer)
        put(RichType.Link.value, RichNodeLink.Drawer)
        put(RichType.Topic.value, RichNodeTopic.Drawer)
        put(RichType.At.value, RichNodeAt.Drawer)
        put(RichType.Style.value, RichNodeStyle.Drawer)
        for (drawer in drawers) put(drawer.type, drawer)
    }

    fun addDrawer(drawer: RichDrawer) {
        renderDrawers[drawer.type] = drawer
    }

    internal fun render(fontSize: TextUnit, source: RichString): RichRenderResult {
        val scope = RichRenderScope(renderDrawers, fontSize, onAction)
        scope.renderList(source.items)
        return scope.build()
    }
}

@Composable
fun rememberRichRenderer(
    drawerProvider: () -> List<RichDrawer> = { emptyList() },
    onAction: (RichObject) -> Unit  = {}
): RichRenderer {
    val onActionUpdate by rememberUpdatedState(onAction)
    return remember { RichRenderer(drawerProvider(), onActionUpdate) }
}