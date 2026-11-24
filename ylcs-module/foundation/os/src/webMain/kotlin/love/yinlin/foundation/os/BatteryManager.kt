@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.foundation.os

import kotlinx.coroutines.await
import love.yinlin.extension.cast
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import kotlin.js.JsNumber
import kotlin.js.Promise

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
        val promise = requestBatteryLevel() ?: return@catchingDefault BatteryLevel.Unsupported
        BatteryLevel.fromFraction(promise.await().cast)
    }
}

private fun requestBatteryLevel(): Promise<JsNumber>? = js("""
    typeof navigator !== 'undefined' && typeof navigator.getBattery === 'function'
        ? navigator.getBattery().then(function(battery) { return battery.level; })
        : null
""")
