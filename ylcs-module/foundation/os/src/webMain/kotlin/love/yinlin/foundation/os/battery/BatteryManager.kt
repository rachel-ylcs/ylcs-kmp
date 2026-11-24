@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.foundation.os.battery

import kotlinx.coroutines.await
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.time.Duration.Companion.seconds

@Suppress("RedundantNullableReturnType")
private fun requestBattery(): Promise<JsAny>? = js("""
    typeof navigator !== 'undefined' && typeof navigator.getBattery === 'function' ? navigator.getBattery() : null
""")
private fun batteryLevel(battery: JsAny): Double = js("battery.level")
private fun batteryCharging(battery: JsAny): Boolean = js("battery.charging")
private fun batteryDischargingTime(battery: JsAny): Double = js("battery.dischargingTime")

actual object BatteryManager {
    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = catchingDefault(BatteryInfo()) {
        val request = requestBattery() ?: return@catchingDefault BatteryInfo(level = BatteryLevel.Unsupported)

        val battery = request.await()
        val charging = batteryCharging(battery)
        val fraction = batteryLevel(battery)
        val remaining = batteryDischargingTime(battery).takeIf { !charging && it.isFinite() && it >= 0.0 }?.seconds

        BatteryInfo(
            level = BatteryLevel.fromFraction(fraction),
            chargingState = when {
                !charging -> BatteryChargingState.Discharging
                fraction == 1.0 || fraction !in 0.0 .. 1.0 -> null
                else -> BatteryChargingState.Charging
            },
            remainingTime = remaining
        )
    }
}
