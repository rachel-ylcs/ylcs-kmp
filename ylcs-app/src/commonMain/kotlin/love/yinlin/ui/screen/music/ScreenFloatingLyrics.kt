package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.music.FloatingLyricsConfig
import love.yinlin.extension.OffScreenEffect
import love.yinlin.platform.app
import love.yinlin.ui.component.input.BeautifulSlider
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenFloatingLyrics(model: AppModel) : CommonSubScreen(model) {
    private var enabled: Boolean by mutableStateOf(app.musicFactory.floatingLyrics?.isAttached ?: false)
    private var config: FloatingLyricsConfig by mutableStateOf(app.config.floatingLyricsConfig)

    private fun enableFloatingLyrics(value: Boolean) {
        app.musicFactory.floatingLyrics?.let { floatingLyrics ->
            if (value) {
                if (floatingLyrics.canAttached) {
                    floatingLyrics.attach()
                    launch {
                        delay(300)
                        enabled = floatingLyrics.isAttached
                    }
                }
                else floatingLyrics.applyPermission { enabled = it }
            }
            else {
                floatingLyrics.detach()
                launch {
                    delay(300)
                    enabled = floatingLyrics.isAttached
                }
            }
        }
    }

    @Composable
    private fun LineLayout(
        title: String,
        content: @Composable () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace * 2),
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

    override val title: String = "悬浮歌词"

    @Composable
    override fun SubContent(device: Device) {
        Column(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .padding(ThemeValue.Padding.EqualExtraValue)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            LineLayout("悬浮歌词模式") {
                Switch(
                    checked = enabled,
                    onCheckedChange = { enableFloatingLyrics(it) }
                )
            }
            LineLayout("左侧偏移") {
                BeautifulSlider(
                    value = config.leftProgress,
                    height = ThemeValue.Size.SliderHeight,
                    onValueChange = { config = config.copyLeft(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            LineLayout("右侧偏移") {
                BeautifulSlider(
                    value = config.rightProgress,
                    height = ThemeValue.Size.SliderHeight,
                    onValueChange = { config = config.copyRight(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            LineLayout("顶部偏移") {
                BeautifulSlider(
                    value = config.topProgress,
                    height = ThemeValue.Size.SliderHeight,
                    onValueChange = { config = config.copyTop(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            LineLayout("字体大小") {
                BeautifulSlider(
                    value = config.textSizeProgress,
                    height = ThemeValue.Size.SliderHeight,
                    onValueChange = { config = config.copyTextSize(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
//            LineLayout("字体颜色") {
//
//            }
//            LineLayout("背景颜色") {
//
//            }
        }

        OffScreenEffect {
            app.musicFactory.floatingLyrics?.let { floatingLyrics ->
                if (!floatingLyrics.canAttached && floatingLyrics.isAttached) {
                    floatingLyrics.detach()
                    launch {
                        delay(300)
                        enabled = floatingLyrics.isAttached
                    }
                }
                else {
                    if (floatingLyrics.canAttached && !floatingLyrics.isAttached) {
                        floatingLyrics.attach()
                        launch {
                            delay(300)
                            enabled = floatingLyrics.isAttached
                        }
                    }
                    else enabled = floatingLyrics.isAttached
                }
            }
        }
    }
}