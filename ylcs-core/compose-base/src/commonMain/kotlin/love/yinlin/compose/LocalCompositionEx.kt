package love.yinlin.compose

import androidx.compose.runtime.staticCompositionLocalOf

fun <T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present") }