package androidx.compose.ui.text.font

import androidx.compose.ui.text.ExperimentalTextApi
import kotlin.coroutines.CoroutineContext

/** Creates a resolver using the registered text backend outside a composition. */
expect fun createFontFamilyResolver(): FontFamily.Resolver

/** Creates a resolver whose asynchronous font work uses [coroutineContext]. */
@ExperimentalTextApi
expect fun createFontFamilyResolver(coroutineContext: CoroutineContext): FontFamily.Resolver
