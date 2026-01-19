package love.yinlin.compose.extension

import androidx.compose.runtime.staticCompositionLocalOf
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

@OptIn(CompatibleRachelApi::class)
inline fun <reified T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present Type: ${metaClassName<T>()}") }

fun <T> localComposition(default: () -> T) = staticCompositionLocalOf(default)