package love.yinlin.compose.extension

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

@OptIn(CompatibleRachelApi::class)
inline fun <reified T> staticLocalComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present Type: ${metaClassName<T>()}") }

fun <T> staticLocalComposition(default: () -> T) = staticCompositionLocalOf(default)

@OptIn(CompatibleRachelApi::class)
inline fun <reified T> localComposition() = compositionLocalOf<T> { error("CompositionLocal not present Type: ${metaClassName<T>()}") }

fun <T> localComposition(default: () -> T) = compositionLocalOf(structuralEqualityPolicy(), default)

@OptIn(CompatibleRachelApi::class)
inline fun <reified T> localRefComposition() = compositionLocalOf<T> { error("CompositionLocal not present Type: ${metaClassName<T>()}") }

fun <T> localRefComposition(default: () -> T) = compositionLocalOf(referentialEqualityPolicy(), default)