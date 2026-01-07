package love.yinlin.platform

import androidx.compose.runtime.Stable

// MMKV 过期时间单位是秒
@Stable
data object KVExpire {
    const val NEVER = 0
    const val MINUTE = 60
    const val HOUR = 3600
    const val DAY = 86400
    const val MONTH = 2592000
    const val YEAR = 946080000
}