package love.yinlin.service

import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class StartupArg<T : Any>(val index: Int, val name: String, val clz: KClass<T>)