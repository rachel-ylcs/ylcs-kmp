package love.yinlin.fixup

import kotlinx.coroutines.delay
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform

// ignoresMouseEvents on macOS is buggy
// See https://stackoverflow.com/questions/29441015
data object FixupMacOSMouseClick {
    inline fun setupDelay(value: Boolean, crossinline setValue: (Boolean) -> Unit) = Platform.use(Platform.MacOS) {
        if (value) {
            setValue(false)
            Coroutines.startMain {
                delay(100)
                setValue(true)
            }
        }
    }
}