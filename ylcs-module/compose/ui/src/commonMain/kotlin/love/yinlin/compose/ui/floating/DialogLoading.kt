package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.text.Text

@Stable
class DialogLoading : DialogTemplate<Unit>() {
    override val dismissOnBackPress: Boolean = false
    override val dismissOnClickOutside: Boolean = false
    override val icon: ImageVector = Icons.Refresh
    override val scrollable: Boolean = false
    override val contentAlignment: Alignment = Alignment.Center

    private var title: String? by mutableStateOf(ValueTheme.runtime())
    private var content: String by mutableStateOf("")

    suspend fun open(content: String = "", title: String? = ValueTheme.runtime(), block: suspend DialogLoading.() -> Unit) {
        this.title = title
        this.content = content
        awaitResult(this, block)
    }

    @Composable
    override fun Land() {
        LandDialogTemplate(title ?: Theme.value.dialogLoadingText) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
            ) {
                CircleLoading.Content()
                Text(text = content)
            }
        }
    }
}