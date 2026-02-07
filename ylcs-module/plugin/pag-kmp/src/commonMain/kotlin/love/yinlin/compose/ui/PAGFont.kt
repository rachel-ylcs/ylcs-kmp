package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import kotlin.String

@Stable
expect class PAGFont(fontFamily: String = "", fontStyle: String = "") {
    companion object {
        fun registerFont(path: String, ttcIndex: Int = 0, font: PAGFont = PAGFont()): PAGFont?
        fun registerFont(bytes: ByteArray, ttcIndex: Int = 0, font: PAGFont = PAGFont()): PAGFont?
        fun unregisterFont(font: PAGFont)
    }

    val fontFamily: String
    val fontStyle: String
}