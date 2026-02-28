package love.yinlin.compose

import androidx.compose.runtime.Stable

@Stable
data class ToolingTheme(
    val enableBallonTip: Boolean
) {
    companion object {
        val Default: ToolingTheme = ToolingTheme(
            enableBallonTip = false
        )
    }
}