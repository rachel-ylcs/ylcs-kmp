package love.yinlin.compose.image.data

import android.graphics.drawable.AnimatedImageDrawable
import java.lang.ref.WeakReference

// 缓存不强持有系统播放器；只允许一个消费者接管，其他位置建立独立播放器
internal actual class PlatformImageAnimation private constructor(
    private val lease: AndroidAnimationLease?,
    private val reference: WeakReference<AndroidAnimationLease>,
) {
    private constructor(lease: AndroidAnimationLease) : this(lease, WeakReference(lease))
    constructor(drawable: AnimatedImageDrawable) : this(AndroidAnimationLease(drawable))
    fun acquire(): AnimatedImageDrawable? = (lease ?: reference.get())?.acquire()
    actual fun cacheCopy(): PlatformImageAnimation = PlatformImageAnimation(null, reference)
}