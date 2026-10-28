@file:Suppress("DEPRECATION", "INVISIBLE_MEMBER")
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalNativeApi::class)
package androidx.compose.ui.platform

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.EagerInitialization
import kotlin.native.OsFamily
import kotlin.native.Platform

internal class NativeMainDispatcher : MainCoroutineDispatcher() {
    private val lock = SynchronizedObject()
    private var host: NativeUiDispatcher? = null
    private var started = false
    private val pending = ArrayDeque<Pair<CoroutineContext, Runnable>>()

    override val immediate: MainCoroutineDispatcher = object : MainCoroutineDispatcher() {
        override val immediate: MainCoroutineDispatcher get() = this
        override fun isDispatchNeeded(context: CoroutineContext): Boolean = synchronized(lock) { host?.isDispatchThread() != true }
        override fun dispatch(context: CoroutineContext, block: Runnable) = this@NativeMainDispatcher.dispatch(context, block)
        override fun toString(): String = "Dispatchers.Main.immediate"
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val dispatcher = synchronized(lock) {
            if (host == null && !started) {
                pending.addLast(context to block)
                return
            }
            host
        }
        if (dispatcher != null) dispatcher.dispatch(context, block)
        else {
            context[Job]?.cancel(CancellationException("Native Compose UI event loop is closed"))
            Dispatchers.Default.dispatch(context, block)
        }
    }

    fun attach(dispatcher: NativeUiDispatcher) {
        check(dispatcher.isDispatchThread()) { "Attach Dispatchers.Main on the Native UI host thread" }
        synchronized(lock) {
            check(host == null) { "Dispatchers.Main already has a Native Compose UI host" }
            host = dispatcher
            started = true
            while (true) {
                val [context, block] = pending.removeFirstOrNull() ?: break
                dispatcher.dispatch(context, block)
            }
        }
    }

    fun detach(dispatcher: NativeUiDispatcher) {
        synchronized(lock) { if (host === dispatcher) host = null }
    }

    override fun toString(): String = "Dispatchers.Main"
}

// Coroutines 1.11.0 has no public Native Main factory SPI. Keep its published injection hook
// isolated here and install before application-level MainScope properties capture Dispatchers.Main.
// Darwin already provides a system Main dispatcher and must retain it.
@EagerInitialization
internal val nativeMainDispatcher: NativeMainDispatcher? = when (Platform.osFamily) {
    OsFamily.WINDOWS, OsFamily.LINUX -> {
        val dispatcher = NativeMainDispatcher()
        @Suppress("INVISIBLE_REFERENCE")
        Dispatchers.injectMain(dispatcher)
        dispatcher
    }
    else -> null
}
