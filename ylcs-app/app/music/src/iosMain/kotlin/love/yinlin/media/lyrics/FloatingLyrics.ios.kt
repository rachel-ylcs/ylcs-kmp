package love.yinlin.media.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.foundation.Context

// TODO: ios部分需要重新实现, 把 platform 相关的代码全部移入 swift
// 涉及到 view、初始化、布局、attach 等等都放在原生上
@Stable
actual class FloatingLyrics {
    actual var isAttached: Boolean = false

    actual fun attach() { }

    actual fun detach() { }

    actual suspend fun initDelay(context: Context) { }

    actual fun update() { }

    @Composable
    actual fun Content() { }
}