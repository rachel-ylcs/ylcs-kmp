package love.yinlin.reflect

import love.yinlin.annotation.CompatibleRachelApi
import kotlin.reflect.KClass

@CompatibleRachelApi
actual val KClass<*>.metaRawClassName: String get() = this.simpleName!!

@CompatibleRachelApi
actual inline fun <reified T> metaClassName(): String = T::class.simpleName!!

@CompatibleRachelApi
actual inline val <T : Any> T.metaClassName: String get() = this::class.simpleName!!

@CompatibleRachelApi
actual val KClass<*>.metaRawIsAnonymousClass: Boolean get() = this.simpleName == null

@CompatibleRachelApi
actual inline fun <reified T> metaIsAnonymousClass(): Boolean = T::class.simpleName == null

@CompatibleRachelApi
actual inline val <T : Any> T.metaIsAnonymousClass: Boolean get() = this::class.simpleName == null