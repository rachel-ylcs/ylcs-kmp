package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme

@Stable
sealed interface AnsiColor {
    @Stable
    data class Indexed(val index: Int) : AnsiColor
    @Stable
    data class Rgb(val red: Int, val green: Int, val blue: Int) : AnsiColor

    companion object {
        private val ResolveSteps: IntArray = [0, 95, 135, 175, 215, 255]

        @Composable
        internal fun resolve(color: AnsiColor?): Color? = when (color) {
            null -> null
            is Rgb -> Color(color.red, color.green, color.blue)
            is Indexed -> when {
                color.index < 16 -> [
                    LocalColor.current, Theme.color.error, Theme.color.secondary, Theme.color.warning,
                    Theme.color.primary, Theme.color.tertiary, Theme.color.secondary, LocalColorVariant.current
                ][color.index % 8]
                color.index < 232 -> {
                    val v = color.index - 16
                    Color(ResolveSteps[v / 36], ResolveSteps[(v / 6) % 6], ResolveSteps[v % 6])
                }
                else -> {
                    val v = 8 + (color.index - 232) * 10
                    Color(v, v, v)
                }
            }
        }
    }
}