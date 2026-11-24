package love.yinlin.foundation.os.battery

/**
 * 充电状态
 */
enum class BatteryChargingState {
    Charging, // 正在充电
    Discharging, // 正在放电
    Full, // 已充满
    NotCharging, // 未在充电
}
