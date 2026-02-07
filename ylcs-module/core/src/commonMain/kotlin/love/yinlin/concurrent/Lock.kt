package love.yinlin.concurrent

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized as sync

@OptIn(InternalCoroutinesApi::class)
class Lock {
    @PublishedApi
    internal val obj = SynchronizedObject()

    inline fun <R> synchronized(block: () -> R) = sync(obj, block)
}