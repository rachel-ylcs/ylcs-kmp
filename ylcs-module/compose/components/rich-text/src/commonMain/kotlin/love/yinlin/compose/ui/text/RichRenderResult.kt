package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString

@Stable
internal class RichRenderResult(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)