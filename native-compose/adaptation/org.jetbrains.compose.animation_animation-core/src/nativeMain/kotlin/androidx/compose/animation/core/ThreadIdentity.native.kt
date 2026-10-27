package androidx.compose.animation.core

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private val animationThreadIdentity = Any()

internal actual fun getCurrentThread(): Any = animationThreadIdentity
