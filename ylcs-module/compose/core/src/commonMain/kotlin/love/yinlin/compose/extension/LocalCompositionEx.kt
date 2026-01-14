package love.yinlin.compose.extension

import androidx.compose.runtime.staticCompositionLocalOf

inline fun <reified T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present Type: ${T::class.qualifiedName}") }

fun <T> localComposition(default: () -> T) = staticCompositionLocalOf(default)