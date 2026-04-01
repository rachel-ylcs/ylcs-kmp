package love.yinlin.reflect

import love.yinlin.annotation.CompatibleRachelApi
import kotlin.reflect.KClass

@CompatibleRachelApi
actual val KClass<*>.metaRawClassName: String get() = this.qualifiedName!!

@CompatibleRachelApi
actual inline fun <reified T> metaClassName(): String = T::class.qualifiedName!!

@CompatibleRachelApi
actual inline val <T : Any> T.metaClassName: String get() = this::class.qualifiedName!!

@CompatibleRachelApi
actual val KClass<*>.metaRawIsAnonymousClass: Boolean get() = this.qualifiedName == null

@CompatibleRachelApi
actual inline fun <reified T> metaIsAnonymousClass(): Boolean = T::class.qualifiedName == null

@CompatibleRachelApi
actual inline val <T : Any> T.metaIsAnonymousClass: Boolean get() = this::class.qualifiedName == null