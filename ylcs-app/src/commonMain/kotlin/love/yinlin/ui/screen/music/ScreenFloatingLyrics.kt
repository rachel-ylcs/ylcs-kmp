package love.yinlin.ui.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.music.FloatingLyricsConfig
import love.yinlin.extension.OffScreenEffect
import love.yinlin.platform.app
import love.yinlin.ui.component.input.BeautifulSlider
import love.yinlin.ui.component.input.DockedColorPicker
import love.yinlin.ui.component.layout.SplitLayout
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
    private fun RowLayout(
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

    @Composable
    private fun ColumnLayout(
        title: String,
        content: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace * 2)
        ) {
            Text(text = title)
            content()
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
            RowLayout("悬浮歌词模式") {
                Switch(
                    checked = enabled,
                    onCheckedChange = { enableFloatingLyrics(it) }
                )
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.padding(
                        start = this.maxWidth * config.left.coerceIn(0f, 1f),
                        end = this.maxWidth * (1 - config.right).coerceIn(0f, 1f),
                        top = ThemeValue.Padding.VerticalExtraSpace * 4f * config.top
                    ).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "这是一条测试歌词~",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
                        ),
                        color = Color(config.textColor),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.background(color = Color(config.backgroundColor)).padding(ThemeValue.Padding.Value)
                    )
                }
            }
            RowLayout("左侧偏移") {
                BeautifulSlider(
                    value = config.leftProgress,
                    onValueChange = { config = config.copyLeft(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            RowLayout("右侧偏移") {
                BeautifulSlider(
                    value = config.rightProgress,
                    onValueChange = { config = config.copyRight(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            RowLayout("顶部偏移") {
                BeautifulSlider(
                    value = config.topProgress,
                    onValueChange = { config = config.copyTop(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            RowLayout("字体大小") {
                BeautifulSlider(
                    value = config.textSizeProgress,
                    onValueChange = { config = config.copyTextSize(it) },
                    onValueChangeFinished = { app.config.floatingLyricsConfig = config },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }
            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
                gap = ThemeValue.Padding.HorizontalExtraSpace * 2,
                left = {
                    ColumnLayout("字体颜色") {
                        DockedColorPicker(
                            initialColor = Color(app.config.floatingLyricsConfig.textColor),
                            onColorChanged = { config = config.copy(textColor = it.value) },
                            onColorChangeFinished = { app.config.floatingLyricsConfig = config },
                            modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                        )
                    }
                },
                right = {
                    ColumnLayout("背景颜色") {
                        DockedColorPicker(
                            initialColor = Color(app.config.floatingLyricsConfig.backgroundColor),
                            onColorChanged = { config = config.copy(backgroundColor = it.value) },
                            onColorChangeFinished = { app.config.floatingLyricsConfig = config },
                            modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                        )
                    }
                }
            )
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