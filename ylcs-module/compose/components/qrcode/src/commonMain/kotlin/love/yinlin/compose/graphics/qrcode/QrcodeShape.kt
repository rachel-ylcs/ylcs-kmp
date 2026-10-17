package love.yinlin.compose.graphics.qrcode

import androidx.compose.runtime.Stable

/** 二维码码点和定位图案的样式。 */
@Stable
enum class QrcodeShape {
    Square, // 方形码点、定位框和定位点
    Rounded; // 相邻码点连接，仅外角取圆角；定位框使用小外圆角，内框与定位点使用圆角方形。
}
