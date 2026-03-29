package love.yinlin.reflect

import love.yinlin.annotation.CompatibleRachelApi
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

val KClass<*>.metaRawSimpleClassName: String get() = this.simpleName!!

inline fun <reified T> metaSimpleClassName(): String = T::class.simpleName!!

val <T : Any> T.metaSimpleClassName: String get() = this::class.simpleName!!

@get:JvmName("metaSimpleClassNameByType")
inline val <reified T> T.metaSimpleClassName: String get() = T::class.simpleName!!

@CompatibleRachelApi
expect val KClass<*>.metaRawClassName: String

@CompatibleRachelApi
expect inline fun <reified T> metaClassName(): String

@CompatibleRachelApi
expect inline val <T : Any> T.metaClassName: String

@Suppress("UnusedReceiverParameter")
@CompatibleRachelApi
@get:JvmName("metaClassNameByType")
inline val <reified T> T.metaClassName: String get() = metaClassName<T>()

@CompatibleRachelApi
expect val KClass<*>.metaRawIsAnonymousClass: Boolean

@CompatibleRachelApi
expect inline fun <reified T> metaIsAnonymousClass(): Boolean

@CompatibleRachelApi
expect inline val <T : Any> T.metaIsAnonymousClass: Boolean

@Suppress("UnusedReceiverParameter")
@CompatibleRachelApi
@get:JvmName("metaIsAnonymousClassByType")
inline val <reified T> T.metaIsAnonymousClass: Boolean get() = metaIsAnonymousClass<T>()