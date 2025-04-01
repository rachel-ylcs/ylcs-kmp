package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import love.yinlin.extension.DateEx
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSException
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
class ActualAppContext : AppContext() {
	private val screenBounds = UIScreen.mainScreen.bounds

	override val screenWidth: Int = CGRectGetWidth(screenBounds).toInt()
	override val screenHeight: Int = CGRectGetHeight(screenBounds).toInt()
	override val fontScale: Float = 1f
	override val kv: KV = KV()

	@OptIn(ExperimentalForeignApi::class)
	private val exceptionHandler = NSUncaughtExceptionHandler({ e: NSException? ->
		e?.let { ex ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${ex.reason}\n${ex.callStackSymbols.joinToString(",")}")
		}
	}.objcPtr())

	override fun initialize(): AppContext {
		super.initialize()
		// 注册异常回调
		NSSetUncaughtExceptionHandler(exceptionHandler.ptr)
		return this
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext