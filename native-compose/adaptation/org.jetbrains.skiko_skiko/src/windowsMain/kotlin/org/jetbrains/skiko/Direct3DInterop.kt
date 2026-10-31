package org.jetbrains.skiko

import org.jetbrains.skia.DirectContext
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private var renderingDirect3DContext: DirectContext? = null

/** Borrowed from the active Native renderer; use on its render thread and never close it. */
val currentDirect3DContext: DirectContext? get() = renderingDirect3DContext

internal inline fun <T> withDirect3DContext(context: DirectContext, action: () -> T): T {
    val previous = renderingDirect3DContext
    renderingDirect3DContext = context
    try {
        return action()
    }
    finally {
        renderingDirect3DContext = previous
    }
}
