@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.foundation.os.system

import kotlinx.coroutines.await
import love.yinlin.extension.catchingNull
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js

private fun browserPlatform(): String? = js("""
    (function() {
        if (typeof navigator === 'undefined') return null;
        const userAgent = navigator.userAgent || '';
        const userAgentData = navigator.userAgentData;
        const clientPlatform = userAgentData ? userAgentData.platform : '';
        if (clientPlatform) return clientPlatform;
        if (/Android/i.test(userAgent)) return 'Android';
        if (/(iPhone|iPad|iPod)/i.test(userAgent)) return 'iOS';
        if (/CrOS/i.test(userAgent)) return 'Chrome OS';
        const platform = navigator.platform || '';
        if (/Win/i.test(platform)) return 'Windows';
        if (/Mac/i.test(platform)) return 'macOS';
        if (/Linux/i.test(platform)) return 'Linux';
        return null;
    })()
""")

private fun browserUserAgent(): String = js("typeof navigator !== 'undefined' ? navigator.userAgent || '' : ''")

private fun requestPlatformVersion(): Promise<JsAny>? = js("""
    typeof navigator !== 'undefined' &&
    navigator.userAgentData &&
    typeof navigator.userAgentData.getHighEntropyValues === 'function'
        ? navigator.userAgentData.getHighEntropyValues(['platformVersion'])
        : null
""")

private suspend fun browserPlatformVersion(): String? {
    val request = requestPlatformVersion() ?: return null
    return catchingNull {
        val values = request.await()
        readPlatformVersion(values)
    }
}

private fun readPlatformVersion(values: JsAny): String? = js("values.platformVersion || null")

actual object SystemManager {
    private suspend fun formatSystemVersion(platform: String, version: String?, userAgent: String): String? = when (platform) {
        "Windows" -> {
            val major = version?.substringBefore('.')?.toIntOrNull()
            when {
                major != null && major >= 13 -> "Windows 11"
                major != null && major in 1 .. 10 -> "Windows 10"
                else -> {
                    val ntVersion = Regex("Windows NT ([0-9.]+)").find(userAgent)?.groupValues?.get(1)
                    when (ntVersion) {
                        "6.3" -> "Windows 8.1"
                        "6.2" -> "Windows 8"
                        "6.1" -> "Windows 7"
                        "6.0" -> "Windows Vista"
                        "5.2" -> "Windows XP x64 / Server 2003"
                        "5.1" -> "Windows XP"
                        "5.0" -> "Windows 2000"
                        else -> "Windows"
                    }
                }
            }
        }
        "macOS" -> version?.takeIf { it.isNotBlank() }?.let { "macOS $it" } ?: "macOS"
        "Android" -> version?.substringBefore('.')?.takeIf { it.isNotBlank() }?.let { "Android $it" } ?: "Android"
        "iOS" -> Regex("(?:iPhone OS|CPU OS) ([0-9_]+)").find(userAgent)?.groupValues?.get(1)?.replace('_', '.')?.let { "iOS $it" } ?: "iOS"
        "Chrome OS" -> Regex("CrOS [^ ]+ ([0-9.]+)").find(userAgent)?.groupValues?.get(1)?.let { "Chrome OS $it" } ?: "Chrome OS"
        "Linux" -> "Linux"
        else -> platform.takeIf { it.isNotBlank() }
    }

    actual suspend fun getSystemVersion(): String? {
        val platform = browserPlatform() ?: return null
        return formatSystemVersion(platform, browserPlatformVersion(), browserUserAgent())
    }

    actual suspend fun getUserName(): String? = null
}
