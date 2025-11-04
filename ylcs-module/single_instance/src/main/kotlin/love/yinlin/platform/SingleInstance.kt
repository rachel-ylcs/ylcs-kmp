package love.yinlin.platform

import love.yinlin.extension.NativeLib
import kotlin.concurrent.thread
import kotlin.system.exitProcess

internal external fun lockApplication(key: String): Boolean
internal external fun unlockApplication()

@NativeLib
fun singleInstance(key: String) {
    if (!lockApplication(key)) exitProcess(0)
    Runtime.getRuntime().addShutdownHook(thread(start = false) { unlockApplication() })
}