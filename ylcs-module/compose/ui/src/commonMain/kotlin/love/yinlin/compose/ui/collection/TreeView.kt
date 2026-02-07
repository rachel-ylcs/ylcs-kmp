package love.yinlin.compose.ui.collection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.localComposition
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.ExpandableContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.MeasureId
import love.yinlin.compose.ui.layout.find
import love.yinlin.compose.ui.layout.measureId
import love.yinlin.compose.ui.layout.require
import love.yinlin.compose.ui.text.SimpleEllipsisText
import kotlin.math.max

private val LocalTreeNodeColor = localComposition<Color>()
private val LocalTreeNodeStyle = localComposition<TextStyle>()
private val LocalTreeNodePadding = localComposition<PaddingValues>()
private val LocalTreeNodeIndent = localComposition<Dp>()
private val LocalTreeNodeDepth = localComposition<Int>()

private enum class TreeViewMeasureId : MeasureId {
    Text, ExpandIcon, Icon;
}

@Stable
object TreeViewScope {
    @Composable
    private fun TreeRowLayout(
        modifier: Modifier = Modifier,
        depth: Int,
        indent: Dp,
        content: @Composable () -> Unit
    ) {
        Layout(
            content = content,
            modifier = modifier,
        ) { measurables, constraints ->
            val textMeasurable = measurables.require(TreeViewMeasureId.Text)
            val expandIconMeasurable = measurables.find(TreeViewMeasureId.ExpandIcon)
            val iconMeasurable = measurables.find(TreeViewMeasureId.Icon)

            val looseConstraints = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
            val textPlaceable = textMeasurable.measure(looseConstraints)

            val heightPx = textPlaceable.height
            val gap = (heightPx * 0.5f).toInt()

            val iconConstraints = Constraints.fixed(heightPx, heightPx)

            val expandIconPlaceable = expandIconMeasurable?.measure(iconConstraints)
            val iconPlaceable = iconMeasurable?.measure(iconConstraints)

            val expandIconStart = (indent * depth).toPx().toInt()
            val iconStart = expandIconStart + heightPx + gap
            val textStart = iconStart + if (iconPlaceable != null) (heightPx + gap) else 0
            val contentWidth = textStart + textPlaceable.width
            val layoutWidth = max(constraints.minWidth, contentWidth)

            layout(layoutWidth, heightPx) {
                expandIconPlaceable?.placeRelative(expandIconStart, 0)
                iconPlaceable?.placeRelative(iconStart, 0)
                textPlaceable.placeRelative(textStart, 0)
            }
        }
    }


    @Composable
    fun TreeNode(
        text: String,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        onClick: (() -> Unit)? = null,
        color: Color? = null,
        style: TextStyle? = null,
        padding: PaddingValues? = null,
        children: @Composable (TreeViewScope.() -> Unit)? = null,
    ) {
        val expandable = children != null
        var expanded by rememberFalse()

        val nodeColor = color ?: LocalTreeNodeColor.current
        val nodeStyle = style ?: LocalTreeNodeStyle.current
        val nodePadding = padding ?: LocalTreeNodePadding.current

        val depth = LocalTreeNodeDepth.current
        val indent = LocalTreeNodeIndent.current

        Column {
            TreeRowLayout(
                modifier = modifier.fillMaxWidth().clip(Theme.shape.v8).clickable {
                    if (expandable) expanded = !expanded
                    else onClick?.invoke()
                }.padding(nodePadding),
                depth = depth,
                indent = indent
            ) {
                SimpleEllipsisText(
                    text = text,
                    modifier = Modifier.measureId(TreeViewMeasureId.Text),
                    color = nodeColor,
                    style = nodeStyle
                )

                if (expandable) {
                    val expandDegree by animateFloatAsState(
                        targetValue = if (expanded) 90f else 0f,
                        animationSpec = tween(Theme.animation.duration.default)
                    )
                    Icon(icon = Icons.KeyboardArrowRight, color = nodeColor, modifier = Modifier.rotate(expandDegree).measureId(TreeViewMeasureId.ExpandIcon))
                }

                if (icon != null) Icon(icon = icon, color = nodeColor, modifier = Modifier.measureId(TreeViewMeasureId.Icon))
            }
            if (children != null) {
                ExpandableContent(isExpanded = expanded) {
                    CompositionLocalProvider(LocalTreeNodeDepth provides depth + 1) {
                        Column {
                            children()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreeView(
    color: Color = LocalColor.current,
    style: TextStyle = LocalStyle.current,
    padding: PaddingValues = Theme.padding.value,
    indent: Dp = Theme.padding.h10,
    modifier: Modifier = Modifier,
    children: @Composable TreeViewScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalTreeNodeColor provides color,
        LocalTreeNodeStyle provides style,
        LocalTreeNodePadding provides padding,
        LocalTreeNodeIndent provides indent,
        LocalTreeNodeDepth provides 0,
    ) {
        Column(modifier = modifier.width(IntrinsicSize.Max)) {
            TreeViewScope.children()
        }
    }
}