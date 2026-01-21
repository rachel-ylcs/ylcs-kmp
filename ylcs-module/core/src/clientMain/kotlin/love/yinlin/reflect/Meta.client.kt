package love.yinlin.reflect

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
actual inline fun <reified T> metaClassName(): String = T::class.qualifiedName!!

@CompatibleRachelApi
actual val <T : Any> T.metaClassName: String get() = this::class.qualifiedName!!

@CompatibleRachelApi
actual inline fun <reified T> metaIsAnonymousClass(): Boolean = T::class.qualifiedName == null

@CompatibleRachelApi
actual val <T : Any> T.metaIsAnonymousClass: Boolean get() = this::class.qualifiedName == null