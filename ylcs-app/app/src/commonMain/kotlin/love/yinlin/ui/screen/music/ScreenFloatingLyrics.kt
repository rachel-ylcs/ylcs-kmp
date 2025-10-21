package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.compose.*
import love.yinlin.ui.component.screen.CommonSubScreen

@Composable
expect fun ScreenFloatingLyrics.ActualContent(device: Device)

@Stable
class ScreenFloatingLyrics(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "悬浮歌词"

    @Composable
    override fun SubContent(device: Device) {
        ActualContent(device)
    }
}

@Composable
internal fun RowLayout(
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace * 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            content()
        }
    }
}

@Composable
internal fun ColumnLayout(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace * 2)
    ) {
        Text(text = title)
        content()
    }
}