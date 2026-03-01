@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.concurrent.Lock
import org.libpag.PAGInit
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual object PAG {
    actual val sdkVersion: String get() = pagInstance?.SDKVersion() ?: ""

    internal var pagInstance: PlatformPAG? by mutableRefStateOf(null)
    private val pagLock = Lock()

    actual suspend fun init() {
        pagInstance = pagLock.synchronized { pagInstance ?: PAGInit().await() }
    }
}