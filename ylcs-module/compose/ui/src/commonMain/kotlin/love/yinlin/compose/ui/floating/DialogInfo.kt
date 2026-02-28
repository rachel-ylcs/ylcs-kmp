package love.yinlin.compose.ui.floating

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.text.Text

@Stable
class DialogInfo : DialogTemplate<Unit>() {
    override val icon: ImageVector = Icons.Lightbulb
    private var title: String? by mutableStateOf(ValueTheme.runtime())
    private var content: String by mutableStateOf("")

    suspend fun open(content: String, title: String? = ValueTheme.runtime()) {
        this.title = title
        this.content = content
        awaitResult()
    }

    @Composable
    override fun Land() {
        LandDialogTemplate(title = title ?: Theme.value.dialogInfoTitle) {
            Text(text = content)
        }
    }
}