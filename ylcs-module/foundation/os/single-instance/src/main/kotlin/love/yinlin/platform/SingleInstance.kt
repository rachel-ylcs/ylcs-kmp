package love.yinlin.platform

import kotlin.concurrent.thread
import kotlin.system.exitProcess

interface SingleInstance {
    fun lock(key: String): Boolean
    fun unlock()

    companion object {
        fun run(key: String) {
            val instance = when (platform) {
                Platform.Windows -> WindowsSingleInstance
                Platform.Linux -> LinuxSingleInstance
                Platform.MacOS -> MacOSSingleInstance
                else -> error(UnsupportedPlatformText)
            }
            if (!instance.lock(key)) exitProcess(0)
            Runtime.getRuntime().addShutdownHook(thread(start = false) { instance.unlock() })
        }
    }
}