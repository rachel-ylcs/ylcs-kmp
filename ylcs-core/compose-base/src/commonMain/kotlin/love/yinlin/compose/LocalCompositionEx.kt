package love.yinlin.compose

import androidx.compose.runtime.staticCompositionLocalOf

fun <T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present") }
fun <T> localComposition(default: () -> T) = staticCompositionLocalOf(default)