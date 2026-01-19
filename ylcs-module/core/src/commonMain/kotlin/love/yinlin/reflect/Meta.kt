package love.yinlin.reflect

import love.yinlin.annotation.CompatibleRachelApi

inline fun <reified T> metaSimpleClassName(): String = T::class.simpleName!!

val <T : Any> T.metaSimpleClassName: String get() = this::class.simpleName!!

inline val <reified T> T.metaSimpleClassName: String get() = T::class.simpleName!!

@CompatibleRachelApi
expect inline fun <reified T> metaClassName(): String

@CompatibleRachelApi
expect val <T : Any> T.metaClassName: String

@CompatibleRachelApi
inline val <reified T> T.metaClassName: String get() = metaClassName<T>()

@CompatibleRachelApi
expect inline fun <reified T> metaIsAnonymousClass(): Boolean

@CompatibleRachelApi
expect val <T : Any> T.metaIsAnonymousClass: Boolean

@CompatibleRachelApi
inline val <reified T> T.metaIsAnonymousClass: Boolean get() = metaIsAnonymousClass<T>()