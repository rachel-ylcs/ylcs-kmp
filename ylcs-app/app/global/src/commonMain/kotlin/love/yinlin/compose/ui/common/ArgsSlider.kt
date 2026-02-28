package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderFloatConverter
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.input.SliderLongConverter
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.text.SimpleEllipsisText
import kotlin.jvm.JvmName

@Stable
data class SliderArgs<T : Number>(internal val tmpValue: T, val minValue: T, val maxValue: T)

@get:JvmName("SliderArgsIntValue")
val SliderArgs<Int>.value: Int get() = this.tmpValue.coerceIn(this.minValue, this.maxValue)
@get:JvmName("SliderArgsLongValue")
val SliderArgs<Long>.value: Long get() = this.tmpValue.coerceIn(this.minValue, this.maxValue)
@get:JvmName("SliderArgsFloatValue")
val SliderArgs<Float>.value: Float get() = this.tmpValue.coerceIn(this.minValue, this.maxValue)

@Composable
private fun <T : Number> ArgsSliderContainer(
    title: String,
    args: SliderArgs<T>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().dashBorder(Theme.border.v7, Theme.color.primary, Theme.shape.v7).padding(Theme.padding.value),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            SimpleEllipsisText(text = title, style = Theme.typography.v7.bold)
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleEllipsisText(text = "当前: ${args.tmpValue}")
                SimpleEllipsisText(text = "范围: ${args.minValue} ~ ${args.maxValue}")
            }
        }
    }
}

@Composable
@JvmName("ArgsSliderInt")
fun ArgsSlider(
    title: String,
    args: SliderArgs<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ArgsSliderContainer(title = title, args = args, modifier = modifier) {
        Slider(
            value = args.value,
            onValueChangeFinished = onValueChange,
            converter = remember(args) { SliderIntConverter(args.minValue, args.maxValue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@JvmName("ArgsSliderLong")
fun ArgsSlider(
    title: String,
    args: SliderArgs<Long>,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ArgsSliderContainer(title = title, args = args, modifier = modifier) {
        Slider(
            value = args.value,
            onValueChangeFinished = onValueChange,
            converter = remember(args) { SliderLongConverter(args.minValue, args.maxValue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@JvmName("ArgsSliderFloat")
fun ArgsSlider(
    title: String,
    args: SliderArgs<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    ArgsSliderContainer(title = title, args = args, modifier = modifier) {
        Slider(
            value = args.value,
            onValueChangeFinished = onValueChange,
            converter = remember(args) { SliderFloatConverter(args.minValue, args.maxValue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}