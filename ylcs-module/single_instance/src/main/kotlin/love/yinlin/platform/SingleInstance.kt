package love.yinlin.platform

import kotlin.concurrent.thread
import kotlin.system.exitProcess

internal external fun lockApplication(key: String): Boolean
internal external fun unlockApplication()

fun singleInstance(key: String) {
    if (!lockApplication(key)) exitProcess(0)
    Runtime.getRuntime().addShutdownHook(thread(start = false) { unlockApplication() })
}