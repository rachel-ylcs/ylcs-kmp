package love.yinlin.ui.component.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.RichTooltipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import love.yinlin.compose.*
import love.yinlin.platform.app

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallonTip(
    text: String,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(
                contentColor = MaterialTheme.colorScheme.onBackground,
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = CustomTheme.shadow.tonal,
                shadowElevation = CustomTheme.shadow.miniSurface
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        state = rememberTooltipState(),
        enableUserInput = app.config.enabledTip,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallonTip(
    title: String,
    message: String,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            RichTooltip(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = RichTooltipColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionContentColor = MaterialTheme.colorScheme.primary
                ),
                tonalElevation = CustomTheme.shadow.tonal,
                shadowElevation = CustomTheme.shadow.miniSurface
            )
        },
        state = rememberTooltipState(),
        enableUserInput = app.config.enabledTip,
        content = content
    )
}