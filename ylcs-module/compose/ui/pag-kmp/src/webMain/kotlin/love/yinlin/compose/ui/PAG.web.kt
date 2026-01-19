@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.compose.ui

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import org.libpag.PAGInit
import kotlin.js.ExperimentalWasmJsInterop

actual object PAG {
    actual val sdkVersion: String get() = pagInstance?.SDKVersion() ?: ""

    private var pagInstance: PlatformPAG? = null
    private val pagMutex = SynchronizedObject()

    actual suspend fun init() {
        pagInstance = synchronized(pagMutex) { pagInstance ?: PAGInit().await() }
    }
}