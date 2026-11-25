package love.yinlin.foundation.os.system

import platform.UIKit.UIDevice

actual object SystemManager {
    actual suspend fun getSystemVersion(): String? = UIDevice.currentDevice.systemVersion.takeIf { it.isNotBlank() }?.let { "iOS $it" }

    actual suspend fun getUserName(): String? = null
}
