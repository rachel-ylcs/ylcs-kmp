package love.yinlin.compose.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.extension.moveItem

@Composable
internal fun <T> ReorderingList(
    list: List<T>,
    key: (T) -> Any,
    onSettle: (Int, Int) -> Unit,
    modifier: Modifier,
    orientation: Orientation,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable ReorderingItemScope.(T, Boolean) -> Unit,
) {
    val sourceKeys = list.map(key)
    val direction = LocalLayoutDirection.current
    val order = remember(orientation, direction) { sourceKeys.toMutableStateList() }
    val values = list.associateBy(key)
    val container = remember(orientation, direction) { ReorderingListLayout(orientation, sourceKeys) }
    val scope = rememberCoroutineScope()

    val move = rememberUpdatedState<(Int, Int) -> Unit> { from, to ->
        order.moveItem(from, to)
    }
    val settle = rememberUpdatedState<(Int, Int) -> Unit> { from, to ->
        container.expectedSourceKeys = order.toList()
        onSettle(from, to)
    }

    val state = remember(container, scope) {
        ReorderingState(container, scope, 0f, move, settle, settleAfterAnimation = true)
    }

    DisposableEffect(state) {
        onDispose {
            state.cancelDrag()
        }
    }

    val externalChange = sourceKeys != container.sourceKeys && sourceKeys != container.expectedSourceKeys
    val itemKeys = if (externalChange) sourceKeys else order.toList()

    SideEffect {
        if (sourceKeys != container.sourceKeys) {
            if (externalChange) {
                state.cancelDrag()
                order.clear()
                order.addAll(sourceKeys)
            }
            container.sourceKeys = sourceKeys
            container.expectedSourceKeys = null
        }
    }

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            for (itemKey in itemKeys) key(itemKey) {
                ReorderingItemContent(state, itemKey, Modifier, listPlacement(state, itemKey), true) { dragging ->
                    content(values.getValue(itemKey), dragging)
                }
            }
        }
    ) { measurables, constraints ->
        val vertical = orientation == Orientation.Vertical
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val spacing = (if (vertical) verticalArrangement.spacing else horizontalArrangement.spacing).roundToPx()
        val sizes = IntArray(children.size) { if (vertical) children[it].height else children[it].width }
        val mainSize = (sizes.sum() + spacing * (children.size - 1).coerceAtLeast(0)).coerceAtLeast(0)
        val crossSize = children.maxOfOrNull { if (vertical) it.width else it.height } ?: 0
        val width = constraints.constrainWidth(if (vertical) crossSize else mainSize)
        val height = constraints.constrainHeight(if (vertical) mainSize else crossSize)
        val positions = IntArray(children.size)

        if (vertical) with(verticalArrangement) { arrange(height, sizes, positions) }
        else with(horizontalArrangement) { arrange(width, sizes, layoutDirection, positions) }

        val offsets = children.mapIndexed { index, child ->
            if (vertical) Offset(horizontalAlignment.align(child.width, width, layoutDirection).toFloat(), positions[index].toFloat())
            else Offset(positions[index].toFloat(), verticalAlignment.align(child.height, height).toFloat())
        }

        val reversed = !vertical && layoutDirection == LayoutDirection.Rtl

        container.info = ReorderingLayout.Info(
            items = children.mapIndexed { index, child ->
                val offset = offsets[index]
                val logicalOffset = if (reversed) Offset(width - offset.x - child.width, offset.y) else offset
                ReorderingLayout.Item(index, itemKeys[index], logicalOffset, Size(child.width.toFloat(), child.height.toFloat()))
            },
            orientation = orientation,
            viewportSize = (if (vertical) height else width).toFloat(),
            beforeContentPadding = 0,
            totalItemsCount = children.size,
            mainAxisReversed = reversed,
            isList = true,
            crossAxisSize = (if (vertical) width else height).toFloat(),
        )
        layout(width, height) {
            children.forEachIndexed { index, child ->
                child.place(offsets[index].x.toInt(), offsets[index].y.toInt())
            }
        }
    }
}

@Composable
private fun listPlacement(state: ReorderingState, key: Any): Modifier {
    val target by rememberDerivedState(state, key) {
        state.layout.info.let { info -> info.item(key)?.let { info.position(it) } }
    }
    val held by rememberDerivedState(state, key) { state.hasGesture(key) }
    val animation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var placed by rememberFalse()

    LaunchedEffect(target, held) {
        target?.let {
            if (!placed || held) animation.snapTo(it)
            else animation.animateTo(it, spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Offset.VisibilityThreshold))
            placed = true
        }
    }

    return Modifier.graphicsLayer {
        val offset = if (placed && !held) animation.value - (target ?: Offset.Zero) else Offset.Zero
        translationX = offset.x
        translationY = offset.y
    }
}