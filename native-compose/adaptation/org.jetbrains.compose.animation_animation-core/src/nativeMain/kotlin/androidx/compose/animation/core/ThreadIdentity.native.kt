package androidx.compose.animation.core

import kotlin.native.concurrent.ThreadLocal

// Transition compares identities with ===; boxing a numeric thread id would not preserve it.
@ThreadLocal
private val animationThreadIdentity = Any()

internal actual fun getCurrentThread(): Any = animationThreadIdentity
