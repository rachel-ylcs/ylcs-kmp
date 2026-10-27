package org.jetbrains.compose.resources

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal actual class ResourceLock actual constructor() {
    private val lock = SynchronizedObject()

    actual fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
