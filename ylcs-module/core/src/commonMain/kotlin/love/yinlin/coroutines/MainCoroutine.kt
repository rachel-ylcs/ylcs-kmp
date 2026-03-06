package love.yinlin.coroutines

import kotlin.annotation.AnnotationTarget.*

/**
 * Main 协程
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class MainCoroutine
