package love.yinlin.foundation.os.system

import platform.UIKit.UIDevice

actual object SystemManager {
    actual fun getSystemVersion(): String? = UIDevice.currentDevice.systemVersion.takeIf { it.isNotBlank() }?.let { "iOS " + it }

    actual fun getUserName(): String? = null
}
