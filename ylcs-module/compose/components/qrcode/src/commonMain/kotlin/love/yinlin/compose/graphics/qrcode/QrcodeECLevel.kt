package love.yinlin.compose.graphics.qrcode

import androidx.compose.runtime.Stable

/** 二维码的 L、M、Q、H 四级纠错能力；等级越高，可容纳的文本越少。 */
@Stable
enum class QrcodeECLevel(internal val formatBits: Int) {
    L(1),
    M(0),
    Q(3),
    H(2);
}
