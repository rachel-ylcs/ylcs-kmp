@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import love.yinlin.compose.extension.mutableRefStateOf
import org.libpag.PAGInit
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual object PAG {
    actual val sdkVersion: String get() = pagInstance?.SDKVersion() ?: ""

    internal var pagInstance: PlatformPAG? by mutableRefStateOf(null)
    private val pagMutex = SynchronizedObject()

    actual suspend fun init() {
        pagInstance = synchronized(pagMutex) { pagInstance ?: PAGInit().await() }
    }
}