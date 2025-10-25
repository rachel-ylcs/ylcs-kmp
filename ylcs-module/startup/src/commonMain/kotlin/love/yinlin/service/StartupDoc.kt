package love.yinlin.service

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class StartupArg(val index: Int, val name: String, val type: KClass<*>)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class StartupHandler(val index: Int, val name: String, val handlerType: KClass<*>, val returnType: KClass<*>, vararg val argTypes: KClass<*>)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class StartupFetcher(val index: Int, val name: String, val returnType: KClass<*>)