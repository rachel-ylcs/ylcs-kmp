package love.yinlin.platform

import androidx.compose.ui.unit.Dp

expect object Platform {
    // 平台名称
    val platformName: String
    // 屏幕方向
    val isPortrait: Boolean
    // 屏幕密度 ( 设计图 )
    val designDensity: Int
    // 屏幕宽 ( 设计图 )
    val designWidth: Dp
    // 屏幕高 ( 设计图 )
    val designHeight: Dp
}