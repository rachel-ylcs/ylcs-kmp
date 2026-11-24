package love.yinlin.foundation.os

import love.yinlin.foundation.PlatformContext

expect object BatteryManager {
    /**
     * 查询当前电量。Web 无法区分无电池与浏览器报告的满电状态，
     * 因此按照浏览器实际返回的电量处理。
     */
    suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel
}
