package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Fr
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridConfigurationScope
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlin.jvm.JvmInline

@JvmInline
@Stable
value class GridLayoutBuilder(private val raw: GridConfigurationScope) {
    private companion object {
        val Auto = Any()
        val Min = Any()
        val Max = Any()
    }

    @Stable
    private data class MinMax(val min: Dp, val max: Fr)

    val auto get() = Auto
    val min get() = Min
    val max get() = Max

    fun minmax(min: Dp, max: Fr): Any = MinMax(min, max)

    fun buildGridTrackSize(config: Any): GridTrackSize? = when {
        config is Int -> GridTrackSize.Flex(Fr(config.toFloat()))
        config is Float -> GridTrackSize.Percentage(config)
        config is Dp -> GridTrackSize.Fixed(config)
        config is Fr -> GridTrackSize.Flex(config)
        config === Auto -> GridTrackSize.Auto
        config === Min -> GridTrackSize.MinContent
        config === Max -> GridTrackSize.MaxContent
        config is MinMax -> GridTrackSize.MinMax(config.min, config.max)
        else -> null
    }

    fun x(vararg configs: Any) = with(raw) {
        for (config in configs) {
            buildGridTrackSize(config)?.let(::column)
        }
    }

    fun y(vararg configs: Any) = with(raw) {
        for (config in configs) {
            buildGridTrackSize(config)?.let(::row)
        }
    }

    fun sx(value: Dp) = raw.columnGap(value)
    fun sy(value: Dp) = raw.rowGap(value)
    fun s(x: Dp, y: Dp = x) = raw.gap(y, x)

    fun t(label: String, x: Int = 0, y: Int = 0, xs: Int = 1, ys: Int = 1) = raw.area(label, y, x, ys, xs)
}

@JvmInline
@Stable
value class GridLayoutScope(private val raw: GridScope) {
    @Stable
    fun Modifier.item(x: Int = 0, y: Int = 0, xs: Int = 1, ys: Int = 1, alignment: Alignment = Alignment.TopStart) = with(raw) {
        gridItem(y, x, ys, xs, alignment)
    }

    @Stable
    fun Modifier.item(label: String, alignment: Alignment = Alignment.TopStart) = with(raw) { gridItem(label, alignment) }
}

@Composable
inline fun Grid(
    crossinline builder: GridLayoutBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable GridLayoutScope.() -> Unit
) = Grid(
    config = { builder(GridLayoutBuilder(this)) },
    modifier = modifier,
    content = { content(GridLayoutScope(this)) }
)