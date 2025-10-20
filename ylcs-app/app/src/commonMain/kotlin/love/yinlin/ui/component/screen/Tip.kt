package love.yinlin.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import love.yinlin.common.ThemeColor
import love.yinlin.common.ThemeValue
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.node.clickableNoRipple

@Stable
open class Tip(private val scope: CoroutineScope) {
    @Stable
    @Serializable
    enum class Type {
        INFO, SUCCESS, WARNING, ERROR,
    }

    private val host = SnackbarHostState()
    var type: Type by mutableStateOf(Type.INFO)
        private set

    fun show(text: String?, type: Type) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            this@Tip.type = type
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

    @Composable
    fun Land() {
        SnackbarHost(
            hostState = host,
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxWidth()
                .zIndex(Floating.Z_INDEX_TIP)
        ) {
            val color = when (type) {
                Type.INFO -> MaterialTheme.colorScheme.secondaryContainer
                Type.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                Type.WARNING -> ThemeColor.warning
                Type.ERROR -> MaterialTheme.colorScheme.error
            }
            val contentColor = when (type) {
                Type.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
                Type.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                Type.WARNING -> ThemeColor.onWarning
                Type.ERROR -> MaterialTheme.colorScheme.onError
            }
            val tipPadding = PaddingValues(when (LocalDevice.current.size) {
                Device.Size.SMALL -> 40.dp
                Device.Size.MEDIUM -> 50.dp
                Device.Size.LARGE -> 60.dp
            })
            Box(
                modifier = Modifier.padding(tipPadding)
                    .fillMaxWidth()
                    .clickableNoRipple { }
                    .background(color = color, shape = MaterialTheme.shapes.extraLarge)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIcon(
                        icon = when (type) {
                            Type.INFO -> Icons.Outlined.Lightbulb
                            Type.SUCCESS -> Icons.Outlined.Check
                            Type.WARNING -> Icons.Outlined.Warning
                            Type.ERROR -> Icons.Outlined.Error
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
}