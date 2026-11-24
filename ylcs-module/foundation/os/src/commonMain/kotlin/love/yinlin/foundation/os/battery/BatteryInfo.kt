package love.yinlin.foundation.os.battery

import kotlin.time.Duration

/**
 * 电池信息
 */
data class BatteryInfo(
    val level: BatteryLevel = BatteryLevel.Unknown, // 电量
    val chargingState: BatteryChargingState? = null, // 充电状态
    val powerSource: PowerSource? = null, // 供电源
    val remainingTime: Duration? = null, // 电池剩余可用时间
    val isPowerSaveMode: Boolean? = null, // 省电模式
)
