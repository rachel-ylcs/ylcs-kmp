package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.TextUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class RichRenderScope(
    private val drawers: Map<String, RichDrawer>,
    private val fontSize: TextUnit,
    val onAction: (RichObject) -> Unit,
) {
    private var idValue: Int = 0

    private val inlineContents = mutableMapOf<String, InlineTextContent>()

    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    @Suppress("RETURN_VALUE_NOT_USED")
    inline fun <reified T : RichObject> RichObject.cast(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
            (this@cast is T) holdsIn block
        }
        if (this is T) block()
    }

    fun renderCompose(
        width: Float = 1f,
        height: Float = 1f,
        placeholderVerticalAlign: PlaceholderVerticalAlign = PlaceholderVerticalAlign.TextBottom,
        content: @Composable () -> Unit
    ) {
        val id = idValue++.toString()
        val textHeight = fontSize * 1.17f

        builder.appendInlineContent(id, "\uFFFD")
        inlineContents[id] = InlineTextContent(
            placeholder = Placeholder(
                width = textHeight * width,
                height = textHeight * height,
                placeholderVerticalAlign = placeholderVerticalAlign
            )
        ) {
            content()
        }
    }

    fun renderList(items: List<RichObject>) {
        for (item in items) {
            drawers[item.type]?.let { drawer ->
                with(drawer) { render(item) }
            }
        }
    }

    val builder = AnnotatedString.Builder()

    internal fun build(): RichRenderResult = RichRenderResult(builder.toAnnotatedString(), inlineContents)
}