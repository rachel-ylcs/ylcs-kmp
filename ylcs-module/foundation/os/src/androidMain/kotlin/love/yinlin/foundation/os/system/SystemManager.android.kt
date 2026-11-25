package love.yinlin.foundation.os.system

import android.os.Build

actual object SystemManager {
    actual suspend fun getSystemVersion(): String? = Build.VERSION.RELEASE.takeIf { it.isNotBlank() }?.let { "Android $it" }

    actual suspend fun getUserName(): String? = null
}