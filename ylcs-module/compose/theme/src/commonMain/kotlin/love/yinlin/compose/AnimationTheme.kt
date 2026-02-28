package love.yinlin.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Stable

@Stable
data class DurationTheme(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    val v4: Int,
    val v5: Int,
    val v6: Int,
    val v7: Int,
    val v8: Int,
    val v9: Int,
    val v10: Int,
) {
    companion object {
        val Default = DurationTheme(
            v1 = 1500,
            v2 = 1000,
            v3 = 750,
            v4 = 600,
            v5 = 500,
            v6 = 400,
            v7 = 300,
            v8 = 250,
            v9 = 200,
            v10 = 125,
        )
    }

    val default = v6

    fun clone(ratio: Float): DurationTheme = DurationTheme(
        v1 = (v1 * ratio).toInt(),
        v2 = (v2 * ratio).toInt(),
        v3 = (v3 * ratio).toInt(),
        v4 = (v4 * ratio).toInt(),
        v5 = (v5 * ratio).toInt(),
        v6 = (v6 * ratio).toInt(),
        v7 = (v7 * ratio).toInt(),
        v8 = (v8 * ratio).toInt(),
        v9 = (v9 * ratio).toInt(),
        v10 = (v10 * ratio).toInt(),
    )
}

@Stable
data class AnimationTheme(
    val duration: DurationTheme,
    val enter: (duration: Int) -> EnterTransition,
    val exit: (duration: Int) -> ExitTransition,
) {
    companion object {
        val Default: AnimationTheme = AnimationTheme(
            duration = DurationTheme.Default,
            enter = {
                fadeIn(
                    animationSpec = tween(it, it / 2)
                ) + scaleIn(
                    animationSpec = tween(it, it / 2),
                    initialScale = 0.9f
                )
            },
            exit = {
                fadeOut(
                    animationSpec = tween(it / 2)
                )
            }
        )
    }
}