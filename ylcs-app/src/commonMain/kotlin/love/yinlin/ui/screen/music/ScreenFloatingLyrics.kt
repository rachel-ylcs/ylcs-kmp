package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.platform.app
import love.yinlin.ui.component.input.BeautifulSlider
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenFloatingLyrics(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "悬浮歌词"

    private var enabled by mutableStateOf(false)
    private var config by mutableStateOf(app.config.floatingLyricsConfig)

    @Composable
    private fun LineLayout(
        title: String,
        content: @Composable () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
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
    override fun SubContent(device: Device) {
        Column(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .padding(ThemeValue.Padding.EqualValue)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            LineLayout("悬浮歌词模式") {
                Switch(
                    checked = enabled,
                    onCheckedChange = {

                    }
                )
            }
            LineLayout("左侧偏移") {
                BeautifulSlider(
                    value = config.left,
                    onValueChange = {},
                    onValueChangeFinished = {}
                )
            }
            LineLayout("右侧偏移") {
                BeautifulSlider(
                    value = config.right,
                    onValueChange = {},
                    onValueChangeFinished = {}
                )
            }
            LineLayout("顶部偏移") {
                BeautifulSlider(
                    value = config.top,
                    onValueChange = {},
                    onValueChangeFinished = {}
                )
            }
            LineLayout("字体大小") {
                BeautifulSlider(
                    value = config.textSize,
                    onValueChange = {},
                    onValueChangeFinished = {}
                )
            }
            LineLayout("字体颜色") {

            }
            LineLayout("背景颜色") {

            }
        }
    }
}