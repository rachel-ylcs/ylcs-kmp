package love.yinlin.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.common.ThemeColor
import love.yinlin.extension.clickableNoRipple
import love.yinlin.platform.app
import love.yinlin.ui.component.image.MiniIcon

@Stable
open class TipState(private val scope: CoroutineScope) {
    enum class Type {
        INFO, SUCCESS, WARNING, ERROR,
    }

    val host = SnackbarHostState()
    var type by mutableStateOf(Type.INFO)
        private set

    fun show(text: String?, type: Type) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            this@TipState.type = type
            host.showSnackbar(
                message = text ?: "",
                duration = SnackbarDuration.Short
            )
        }
    }

    fun info(text: String?) = show(text, Type.INFO)
    fun success(text: String?) = show(text, Type.SUCCESS)
    fun warning(text: String?) = show(text, Type.WARNING)
    fun error(text: String?) = show(text, Type.ERROR)
}

@Composable
fun Tip(state: TipState) {
    SnackbarHost(
        hostState = state.host,
        modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_TIP)
    ) {
        val color = when (state.type) {
            TipState.Type.INFO -> MaterialTheme.colorScheme.secondaryContainer
            TipState.Type.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
            TipState.Type.WARNING -> ThemeColor.warning
            TipState.Type.ERROR -> MaterialTheme.colorScheme.error
        }
        val contentColor = when (state.type) {
            TipState.Type.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
            TipState.Type.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
            TipState.Type.WARNING -> ThemeColor.onWarning
            TipState.Type.ERROR -> MaterialTheme.colorScheme.onError
        }
        Box(
            modifier = Modifier.padding(
                horizontal = if (app.isPortrait) 20.dp else 100.dp,
                vertical = if (app.isPortrait) 20.dp else 40.dp,
            ).fillMaxWidth()
                .clickableNoRipple { }
                .background(
                    color = color,
                    shape = MaterialTheme.shapes.extraLarge
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniIcon(
                    icon = when (state.type) {
                        TipState.Type.INFO -> Icons.Outlined.Lightbulb
                        TipState.Type.SUCCESS -> Icons.Outlined.Check
                        TipState.Type.WARNING -> Icons.Outlined.Warning
                        TipState.Type.ERROR -> Icons.Outlined.Error
                    },
                    color = contentColor
                )
                Text(
                    text = it.visuals.message,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}