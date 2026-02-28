package love.yinlin.platform

import kotlin.concurrent.thread
import kotlin.system.exitProcess

sealed interface SingleInstanceImpl {
    fun lock(key: String): Boolean
    fun unlock()
}

object SingleInstance : SingleInstanceImpl by when (platform) {
    Platform.Windows -> WindowsSingleInstance
    Platform.Linux -> LinuxSingleInstance
    Platform.MacOS -> MacOSSingleInstance
    else -> error(UnsupportedPlatformText)
} {
    fun run(key: String) {
        if (!lock(key)) exitProcess(0)
        Runtime.getRuntime().addShutdownHook(thread(start = false, block = ::unlock))
    }
}