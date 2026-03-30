package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.FontResource

@Stable
fun interface FontProvider {
    operator fun get(resource: FontResource?): FontFamily

    @Stable
    data object Default : FontProvider {
        override fun get(resource: FontResource?): FontFamily = FontFamily.Default
    }
}