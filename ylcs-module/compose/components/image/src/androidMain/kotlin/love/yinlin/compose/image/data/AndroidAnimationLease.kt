package love.yinlin.compose.image.data

import android.graphics.drawable.AnimatedImageDrawable
import love.yinlin.concurrent.Lock

// 活跃结果持有到首次接管；缓存只弱引用此对象，接管后立即释放播放器引用
internal class AndroidAnimationLease(private var drawable: AnimatedImageDrawable?) {
    private val lock = Lock()

    fun acquire(): AnimatedImageDrawable? = lock.synchronized {
        drawable.also { drawable = null }
    }
}