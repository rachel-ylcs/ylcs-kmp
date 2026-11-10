package love.yinlin.compose.ui.text

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.lerp
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberNull
import kotlin.math.ceil

private const val FLOATING_HINT_SCALE = 0.85f

private val EditorLeft = VerticalAlignmentLine(::minOf)
private val EditorRight = VerticalAlignmentLine(::maxOf)
private val EditorTop = HorizontalAlignmentLine(::minOf)

private fun TextStyle.scaleHint(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
)

@Composable
internal fun InputHintLayout(
    state: InputState,
    hint: String?,
    shape: RoundedCornerShape,
    borderColor: Color,
    content: @Composable (border: Modifier, hintAnchor: Modifier) -> Unit,
) {
    val borderWidth = Theme.border.v6
    val border = Modifier.border(borderWidth, borderColor, shape)

    if (hint.isNullOrEmpty()) {
        content(border, Modifier)
        return
    }

    val style = resolveDefaults(LocalStyle.current, LocalLayoutDirection.current)
    val progress by animateFloatAsState(
        targetValue = if (state.isEmpty) 0f else 1f,
        animationSpec = tween(Theme.animation.duration.v7)
    )
    val measurer = rememberTextMeasurer()
    val inlineWidth = measurer.measure(hint, style).size.width
    val floatingHeight = measurer.measure(hint, style.scaleHint(FLOATING_HINT_SCALE), overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1).size.height
    val gap = Theme.padding.h / 4
    val margin = Theme.padding.h10
    var cutout: Rect? by rememberNull()

    Layout(content = {
        content(Modifier, Modifier.widthIn(min = with(LocalDensity.current) { inlineWidth.toDp() }).layout { measurable, constraints ->
            val hintHeight = measurer.measure(hint, style, constraints = constraints.copy(minWidth = 0, minHeight = 0)).size.height
            val editor = measurable.measure(constraints.copy(minHeight = maxOf(constraints.minHeight, hintHeight)))

            layout(editor.width, editor.height, mapOf(EditorLeft to 0, EditorRight to editor.width, EditorTop to 0)) {
                editor.place(0, 0)
            }
        })

        Box(modifier = Modifier.drawWithContent {
            val opening = cutout
            if (opening == null) drawContent()
            else {
                clipRect(opening.left, opening.top, opening.right, opening.bottom, ClipOp.Difference) {
                    this@drawWithContent.drawContent()
                }
            }
        }.then(border))

        val inline = progress == 0f

        Text(
            text = hint,
            color = LocalColorVariant.current,
            style = style.scaleHint(lerp(1f, FLOATING_HINT_SCALE, progress)),
            overflow = if (inline) TextOverflow.Clip else TextOverflow.Ellipsis,
            softWrap = inline,
            maxLines = if (inline) Int.MAX_VALUE else 1
        )
    }) { measurables, constraints ->
        val topInset = ((floatingHeight + 1) / 2).coerceAtMost(constraints.maxHeight / 2)
        val body = measurables[0].measure(constraints.offset(vertical = -topInset))
        val height = body.height + topInset
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val density = this

        layout(body.width, height) {
            body.place(0, topInset)

            val outline = measurables[1].measure(Constraints.fixed(body.width, body.height))
            val editorLeft = body[EditorLeft].coerceIn(0, body.width)
            val editorRightLine = body[EditorRight]
            val editorRight = if (editorRightLine == AlignmentLine.Unspecified) body.width else editorRightLine.coerceIn(editorLeft, body.width)
            val inlineStart = if (isLtr) editorLeft else body.width - editorRight
            val editorWidth = editorRight - editorLeft
            val editorTop = topInset + body[EditorTop].coerceIn(0, body.height)

            val gapPx = with(density) { gap.roundToPx() }
            val marginPx = with(density) { margin.roundToPx() }
            val bodySize = Size(body.width.toFloat(), body.height.toFloat())
            val startMargin = maxOf(marginPx, shape.topStart.toPx(bodySize, density).toInt() + gapPx)
            val endMargin = maxOf(marginPx, shape.topEnd.toPx(bodySize, density).toInt() + gapPx)
            val floatingWidth = (body.width - startMargin - endMargin).coerceAtLeast(0)
            val labelX = lerp(inlineStart, startMargin, progress).coerceIn(0, body.width)
            val labelWidth = lerp(editorWidth, floatingWidth, progress).coerceIn(0, body.width - labelX)
            val labelMaxHeight = if (progress == 0f) height - editorTop else height
            val label = measurables[2].measure(Constraints(maxWidth = labelWidth, maxHeight = labelMaxHeight))
            val labelY = lerp(editorTop, topInset - label.height / 2, progress).coerceIn(0, height - label.height)
            val borderBottom = with(density) { ceil(borderWidth.toPx()).coerceAtLeast(1f) }

            cutout = if (progress > 0f && label.width > 0 && label.height > 0 && labelY < topInset + borderBottom && labelY + label.height > topInset) {
                val left = if (isLtr) labelX else body.width - labelX - label.width
                Rect((left - gapPx).toFloat(), 0f, (left + label.width + gapPx).toFloat(), borderBottom)
            } else null

            outline.place(0, topInset)
            label.placeRelative(labelX, labelY)
        }
    }
}
