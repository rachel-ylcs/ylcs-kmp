package love.yinlin.reflect

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
actual inline fun <reified T> metaClassName(): String = T::class.simpleName!!

@CompatibleRachelApi
actual inline val <T : Any> T.metaClassName: String get() = this::class.simpleName!!

@CompatibleRachelApi
actual inline fun <reified T> metaIsAnonymousClass(): Boolean = T::class.simpleName == null

@CompatibleRachelApi
actual inline val <T : Any> T.metaIsAnonymousClass: Boolean get() = this::class.simpleName == null