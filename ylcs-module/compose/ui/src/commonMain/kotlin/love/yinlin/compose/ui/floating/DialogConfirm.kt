package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Text

@Stable
class DialogConfirm : DialogTemplate<Unit>() {
    override val icon: ImageVector = Icons.Warning
    private var title: String? by mutableStateOf(ValueTheme.runtime())
    private var content: String by mutableStateOf("")

    override val actions: @Composable (RowScope.() -> Unit) = {
        TextButton(text = Theme.value.dialogYesText, color = Theme.color.primary, onClick = { future?.send(Unit) })
        TextButton(text = Theme.value.dialogNoText, onClick = { close() })
    }

    suspend fun open(content: String, title: String? = ValueTheme.runtime()): Boolean {
        this.title = title
        this.content = content
        return awaitResult() != null
    }

    @Composable
    override fun Land() {
        LandDialogTemplate(title = title ?: Theme.value.dialogConfirmTitle) {
            Text(text = content)
        }
    }
}