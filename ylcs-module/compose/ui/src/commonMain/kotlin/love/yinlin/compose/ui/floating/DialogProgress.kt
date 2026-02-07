package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.status.LinearProgress
import love.yinlin.compose.ui.text.SimpleEllipsisText
import kotlin.math.roundToInt

@Stable
class DialogProgress<R : Any> : DialogTemplate<R>() {
    override val dismissOnBackPress: Boolean = false
    override val dismissOnClickOutside: Boolean = false
    override val scrollable: Boolean = false

    override val icon: ImageVector = Icons.Refresh
    private var title: String? by mutableStateOf(ValueTheme.runtime())

    var current by mutableStateOf("0")
    var total by mutableStateOf("0")
    var progress by mutableFloatStateOf(0f)

    override val actions: @Composable (RowScope.() -> Unit)? = {
        TextButton(text = Theme.value.dialogCancelText, enabled = isOpen, onClick = { close() })
    }

    suspend fun open(title: String? = ValueTheme.runtime(), block: suspend DialogProgress<R>.() -> R): R? {
        this.title = title
        this.current = "0"
        this.total = "0"
        this.progress = 0f
        return awaitResult(this, block)
    }

    @Composable
    override fun Land() {
        LandDialogTemplate(title ?: Theme.value.dialogLoadingText) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)) {
                LinearProgress(
                    value = progress,
                    modifier = Modifier.widthIn(min = minContentWidth)
                )
                Row(
                    modifier = Modifier.widthIn(min = minContentWidth),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = "${(progress * 100).roundToInt()}%")
                    SimpleEllipsisText(text = "$current / $total")
                }
            }
        }
    }
}