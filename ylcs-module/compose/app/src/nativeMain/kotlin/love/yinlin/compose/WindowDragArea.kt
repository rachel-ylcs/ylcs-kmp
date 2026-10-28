package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

@Stable
fun Modifier.windowDragArea(controller: WindowController, draggable: Boolean = true): Modifier = this then WindowDragAreaElement(controller, draggable)

internal class WindowDragAreaNode(var controller: WindowController, var draggable: Boolean) : Modifier.Node(), GlobalPositionAwareModifierNode {
    private var bounds = Rect.Zero

    override fun onAttach() {
        controller.dragAreas.add(this)
    }

    override fun onDetach() {
        controller.dragAreas.remove(this)
        bounds = Rect.Zero
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        bounds = coordinates.boundsInWindow()
    }

    fun contains(x: Float, y: Float): Boolean {
        return isAttached && x >= bounds.left && x < bounds.right && y >= bounds.top && y < bounds.bottom
    }
}

private data class WindowDragAreaElement(val controller: WindowController, val draggable: Boolean) : ModifierNodeElement<WindowDragAreaNode>() {
    override fun create() = WindowDragAreaNode(controller, draggable)

    override fun update(node: WindowDragAreaNode) {
        if (node.controller !== controller) {
            if (node.isAttached) node.controller.dragAreas.remove(node)
            node.controller = controller
            if (node.isAttached) controller.dragAreas.add(node)
        }
        node.draggable = draggable
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "windowDragArea"
        properties["draggable"] = draggable
    }
}
